package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;
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
            GatewayRouter gatewayRouter
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.gatewayRouter = gatewayRouter;
    }

    public TransactionResponse createTransaction(
            TransactionRequest request,
            String email
    ) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethod(),
                TransactionStatus.CREATED,
                user.getId()
        );

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        return new TransactionResponse(savedTransaction);
    }

    public List<TransactionResponse> getUserTransactions(
            String email
    ) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(TransactionResponse::new)
                .toList();
    }

    public TransactionResponse getTransaction(
            Long id,
            String email
    ) {
        Transaction transaction =
                getOwnedTransaction(id, email);

        return new TransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse startProcessing(
            Long id,
            String email
    ) {
        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new IllegalStateException(
                    "Only CREATED transactions can move to PROCESSING"
            );
        }

        /*
         * Select a payment gateway based on the
         * transaction's payment method.
         */
        PaymentGateway gateway =
                gatewayRouter.route(transaction);

        /*
         * Move transaction into PROCESSING state
         * before sending it to the gateway.
         */
        transaction.setStatus(TransactionStatus.PROCESSING);

        transactionRepository.save(transaction);

        /*
         * Simulate payment processing through
         * the selected gateway.
         */
        GatewayResult result =
                gateway.processPayment(transaction);

        /*
         * Update final transaction state based
         * on the gateway result.
         */
        if (result.isSuccess()) {
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
            String email
    ) {
        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only PROCESSING transactions can move to SUCCESS"
            );
        }

        transaction.setStatus(TransactionStatus.SUCCESS);

        return new TransactionResponse(
                transactionRepository.save(transaction)
        );
    }

    @Transactional
    public TransactionResponse markFailed(
            Long id,
            String email
    ) {
        Transaction transaction =
                getOwnedTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only PROCESSING transactions can move to FAILED"
            );
        }

        transaction.setStatus(TransactionStatus.FAILED);

        return new TransactionResponse(
                transactionRepository.save(transaction)
        );
    }

    public void deleteTransaction(
            Long id,
            String email
    ) {
        Transaction transaction =
                getOwnedTransaction(id, email);

        transactionRepository.delete(transaction);
    }

    private Transaction getOwnedTransaction(
            Long id,
            String email
    ) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Transaction transaction =
                transactionRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Transaction not found"
                                )
                        );

        if (!transaction.getUserId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not authorized to access this transaction"
            );
        }

        return transaction;
    }
}