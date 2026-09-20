package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.gateway.GatewayResult;
import com.payflow.payflow_backend.gateway.GatewayRouter;
import com.payflow.payflow_backend.gateway.PaymentGateway;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final GatewayRouter gatewayRouter;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            GatewayRouter gatewayRouter) {

        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.gatewayRouter = gatewayRouter;
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

        /*
         * Check whether this idempotency key has already
         * been used by this user.
         */
        var existingTransaction =
                transactionRepository.findByUserIdAndIdempotencyKey(
                        user.getId(),
                        normalizedKey);

        if (existingTransaction.isPresent()) {

            Transaction existing = existingTransaction.get();

            /*
             * The same idempotency key cannot represent
             * a different payment request.
             */
            if (!isSameRequest(existing, request)) {
                throw new IllegalStateException(
                        "Idempotency key already exists for a different transaction");
            }

            /*
             * Duplicate request:
             * return the original transaction instead of
             * creating another transaction.
             */
            return new TransactionResponse(existing);
        }

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethod(),
                TransactionStatus.CREATED,
                user.getId(),
                normalizedKey
        );

        Transaction savedTransaction =
                transactionRepository.save(transaction);

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
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(TransactionResponse::new)
                .toList();
    }

    public TransactionResponse getTransaction(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

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
         * Get all gateways that support this payment method.
         * This allows us to fail over to another gateway
         * when the first gateway fails.
         */
        List<PaymentGateway> gateways =
                gatewayRouter.routeAll(transaction);

        transaction.setStatus(TransactionStatus.PROCESSING);

        transactionRepository.save(transaction);

        GatewayResult successfulResult = null;

        /*
         * Try gateways one by one.
         *
         * Example:
         *
         * Gateway A -> FAILED
         * Gateway B -> SUCCESS
         *
         * The transaction will ultimately become SUCCESS.
         */
        for (PaymentGateway gateway : gateways) {

            GatewayResult result =
                    gateway.processPayment(transaction);

            if (result.isSuccess()) {
                successfulResult = result;
                break;
            }
        }

        /*
         * If any gateway succeeded, mark the transaction
         * as SUCCESS.
         *
         * If every gateway failed, mark it as FAILED.
         */
        if (successfulResult != null) {
            transaction.setStatus(TransactionStatus.SUCCESS);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        Transaction updatedTransaction =
                transactionRepository.save(transaction);

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

        return new TransactionResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(
            Long id,
            String email) {

        Transaction transaction =
                getOwnedTransaction(id, email);

        transactionRepository.delete(transaction);
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
         * Users can access only their own transactions.
         */
        if (!transaction.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException(
                    "Transaction not found");
        }

        return transaction;
    }
}