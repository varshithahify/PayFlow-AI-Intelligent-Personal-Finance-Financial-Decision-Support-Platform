package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.client.FraudServiceClient;
import com.payflow.payflow_backend.dto.FraudScoreRequest;
import com.payflow.payflow_backend.dto.FraudScoreResponse;
import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.event.FraudAlertEvent;
import com.payflow.payflow_backend.event.FraudAlertProducer;
import com.payflow.payflow_backend.event.TransactionEvent;
import com.payflow.payflow_backend.event.TransactionEventProducer;
import com.payflow.payflow_backend.event.TransactionEventType;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.gateway.GatewayHealthService;
import com.payflow.payflow_backend.gateway.GatewayResult;
import com.payflow.payflow_backend.gateway.GatewayRouter;
import com.payflow.payflow_backend.gateway.PaymentGateway;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final GatewayRouter gatewayRouter;
    private final GatewayHealthService gatewayHealthService;
    private final TransactionEventProducer transactionEventProducer;
    private final TransactionCacheService transactionCacheService;
    private final FraudServiceClient fraudServiceClient;
    private final FraudAlertProducer fraudAlertProducer;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            GatewayRouter gatewayRouter,
            GatewayHealthService gatewayHealthService,
            TransactionEventProducer transactionEventProducer,
            TransactionCacheService transactionCacheService,
            FraudServiceClient fraudServiceClient,
            FraudAlertProducer fraudAlertProducer) {

        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.gatewayRouter = gatewayRouter;
        this.gatewayHealthService = gatewayHealthService;
        this.transactionEventProducer = transactionEventProducer;
        this.transactionCacheService = transactionCacheService;
        this.fraudServiceClient = fraudServiceClient;
        this.fraudAlertProducer = fraudAlertProducer;
    }

    @Transactional
    public TransactionResponse createTransaction(
            TransactionRequest request,
            String email,
            String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }

        String normalizedKey = idempotencyKey.trim();

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        var existingTransaction =
                transactionRepository.findByOrgIdAndUserIdAndIdempotencyKey(
                        user.getOrgId(),
                        user.getId(),
                        normalizedKey);

        if (existingTransaction.isPresent()) {

            Transaction existing = existingTransaction.get();

            if (!isSameRequest(existing, request)) {
                throw new IllegalStateException(
                        "Idempotency key already exists for a different transaction");
            }

            return new TransactionResponse(existing);
        }

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethod(),
                TransactionStatus.CREATED,
                user.getId(),
                user.getOrgId(),
                normalizedKey);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        publishTransactionEvent(
                savedTransaction,
                TransactionEventType.CREATED);

        return new TransactionResponse(savedTransaction);
    }

    private boolean isSameRequest(
            Transaction existing,
            TransactionRequest request) {

        return existing.getAmount().compareTo(request.getAmount()) == 0
                && existing.getCurrency()
                .equalsIgnoreCase(request.getCurrency())
                && existing.getPaymentMethod()
                .equalsIgnoreCase(request.getPaymentMethod());
    }

    public List<TransactionResponse> getUserTransactions(
            String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        return transactionRepository
                .findByOrgIdAndUserIdOrderByCreatedAtDesc(
                        user.getOrgId(),
                        user.getId())
                .stream()
                .map(TransactionResponse::new)
                .toList();
    }

    public TransactionResponse getTransaction(
            Long id,
            String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Transaction cachedTransaction =
                transactionCacheService.getCachedTransaction(
                        user.getId(),
                        id);

        if (cachedTransaction != null) {
            return new TransactionResponse(cachedTransaction);
        }

        Transaction transaction =
                getOwnedTransaction(id, email);

        transactionCacheService.cacheTransaction(
                user.getId(),
                transaction.getId(),
                transaction);

        return new TransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse startProcessing(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new IllegalStateException(
                    "Only CREATED transactions can move to PROCESSING");
        }

        /*
         * Run fraud detection before selecting or calling
         * any payment gateway.
         */
        FraudScoreRequest fraudRequest = new FraudScoreRequest();

        fraudRequest.setTransactionAmt(
                transaction.getAmount().doubleValue());

        /*
         * The current transaction model does not contain
         * card information, so use a stable user identifier
         * for the current velocity check.
         */
        fraudRequest.setCardHash(
                "user:" + transaction.getUserId());

        fraudRequest.setEmail(email);

        FraudScoreResponse fraudResponse =
                fraudServiceClient.score(fraudRequest);

        if (fraudResponse == null) {
            throw new IllegalStateException(
                    "Fraud service returned an empty response");
        }

        /*
         * Publish an alert only for FLAG and BLOCK decisions.
         * ALLOW decisions continue without a fraud alert.
         */
        publishFraudAlertIfNeeded(transaction, fraudResponse);

        /*
         * BLOCK prevents the payment gateway from processing
         * the transaction. FAILED is used because the current
         * TransactionStatus enum has no BLOCKED status.
         */
        if ("BLOCK".equalsIgnoreCase(fraudResponse.getAction())) {

            transaction.setStatus(TransactionStatus.FAILED);

            Transaction updatedTransaction =
                    transactionRepository.save(transaction);

            transactionCacheService.evictTransaction(
                    updatedTransaction.getUserId(),
                    updatedTransaction.getId());

            publishTransactionEvent(
                    updatedTransaction,
                    TransactionEventType.FAILED);

            return new TransactionResponse(updatedTransaction);
        }

        /*
         * ALLOW and FLAG continue through the normal
         * payment gateway flow.
         */
        List<PaymentGateway> gateways =
                gatewayRouter.routeAll(transaction);

        transaction.setStatus(TransactionStatus.PROCESSING);

        transactionRepository.save(transaction);

        transactionCacheService.evictTransaction(
                transaction.getUserId(),
                transaction.getId());

        publishTransactionEvent(
                transaction,
                TransactionEventType.PROCESSING);

        GatewayResult successfulResult = null;

        for (PaymentGateway gateway : gateways) {

            long startTime = System.currentTimeMillis();

            GatewayResult result =
                    gateway.processPayment(transaction);

            int latencyMs =
                    (int) (System.currentTimeMillis() - startTime);

            gatewayHealthService.recordResult(
                    result.getGatewayName(),
                    result.isSuccess(),
                    latencyMs);

            if (result.isSuccess()) {
                successfulResult = result;
                break;
            }
        }

        if (successfulResult != null) {

            transaction.setStatus(TransactionStatus.SUCCESS);

            /*
             * Store the gateway that actually processed
             * the successful payment. Refunds use this gateway.
             */
            transaction.setGatewayName(
                    successfulResult.getGatewayName());

        } else {

            transaction.setStatus(TransactionStatus.FAILED);
        }

        Transaction updatedTransaction =
                transactionRepository.save(transaction);

        transactionCacheService.evictTransaction(
                updatedTransaction.getUserId(),
                updatedTransaction.getId());

        if (updatedTransaction.getStatus()
                == TransactionStatus.SUCCESS) {

            publishTransactionEvent(
                    updatedTransaction,
                    TransactionEventType.SUCCESS);

        } else {

            publishTransactionEvent(
                    updatedTransaction,
                    TransactionEventType.FAILED);
        }

        return new TransactionResponse(updatedTransaction);
    }

    private void publishFraudAlertIfNeeded(
            Transaction transaction,
            FraudScoreResponse fraudResponse) {

        String action = fraudResponse.getAction();

        if (!"FLAG".equalsIgnoreCase(action)
                && !"BLOCK".equalsIgnoreCase(action)) {
            return;
        }

        List<String> triggeredRules =
                fraudResponse.getTriggeredRules() == null
                        ? List.of()
                        : List.copyOf(fraudResponse.getTriggeredRules());

        FraudAlertEvent alertEvent = new FraudAlertEvent(
                UUID.randomUUID(),
                transaction.getId(),
                transaction.getOrgId(),
                action.toUpperCase(),
                fraudResponse.getScore(),
                fraudResponse.getMlScore(),
                fraudResponse.getVelocityScore(),
                triggeredRules,
                LocalDateTime.now());

        fraudAlertProducer.publish(alertEvent);
    }

    @Transactional
    public TransactionResponse refundTransaction(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Only SUCCESS transactions can be refunded");
        }

        if (transaction.getGatewayName() == null
                || transaction.getGatewayName().isBlank()) {
            throw new IllegalStateException(
                    "Original payment gateway is not available");
        }

        PaymentGateway gateway =
                gatewayRouter.getGatewayByName(
                        transaction.getGatewayName());

        GatewayResult refundResult =
                gateway.refundPayment(transaction);

        if (!refundResult.isSuccess()) {
            throw new IllegalStateException(
                    "Refund failed: " + refundResult.getMessage());
        }

        transaction.setStatus(TransactionStatus.REFUNDED);

        Transaction updatedTransaction =
                transactionRepository.save(transaction);

        transactionCacheService.evictTransaction(
                updatedTransaction.getUserId(),
                updatedTransaction.getId());

        publishTransactionEvent(
                updatedTransaction,
                TransactionEventType.REFUNDED);

        return new TransactionResponse(updatedTransaction);
    }

    @Transactional
    public TransactionResponse markSuccess(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only PROCESSING transactions can move to SUCCESS");
        }

        transaction.setStatus(TransactionStatus.SUCCESS);

        Transaction updatedTransaction =
                transactionRepository.save(transaction);

        transactionCacheService.evictTransaction(
                updatedTransaction.getUserId(),
                updatedTransaction.getId());

        publishTransactionEvent(
                updatedTransaction,
                TransactionEventType.SUCCESS);

        return new TransactionResponse(updatedTransaction);
    }

    @Transactional
    public TransactionResponse markFailed(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only PROCESSING transactions can move to FAILED");
        }

        transaction.setStatus(TransactionStatus.FAILED);

        Transaction updatedTransaction =
                transactionRepository.save(transaction);

        transactionCacheService.evictTransaction(
                updatedTransaction.getUserId(),
                updatedTransaction.getId());

        publishTransactionEvent(
                updatedTransaction,
                TransactionEventType.FAILED);

        return new TransactionResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

        transactionRepository.delete(transaction);

        transactionCacheService.evictTransaction(
                transaction.getUserId(),
                transaction.getId());
    }

    private void publishTransactionEvent(
            Transaction transaction,
            TransactionEventType eventType) {

        TransactionEvent event = new TransactionEvent(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getOrgId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                eventType,
                LocalDateTime.now());

        transactionEventProducer.publish(event);
    }

    private Transaction getOwnedTransaction(
            Long id,
            String email) {

        Transaction transaction =
                transactionRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transaction not found"));

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        /*
         * Verify both user and organization ownership.
         */
        if (!transaction.getUserId().equals(user.getId())
                || !transaction.getOrgId().equals(user.getOrgId())) {

            throw new ResourceNotFoundException(
                    "Transaction not found");
        }

        return transaction;
    }
}