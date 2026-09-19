package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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

    public List<TransactionResponse> getUserTransactions(String email) {

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
        Transaction transaction = getUserTransaction(id, email);

        return new TransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse startProcessing(
            Long id,
            String email
    ) {
        Transaction transaction = getUserTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new IllegalStateException(
                    "Only CREATED transactions can move to PROCESSING"
            );
        }

        transaction.setStatus(TransactionStatus.PROCESSING);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        return new TransactionResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse markSuccess(
            Long id,
            String email
    ) {
        Transaction transaction = getUserTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only PROCESSING transactions can move to SUCCESS"
            );
        }

        transaction.setStatus(TransactionStatus.SUCCESS);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        return new TransactionResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse markFailed(
            Long id,
            String email
    ) {
        Transaction transaction = getUserTransaction(id, email);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only PROCESSING transactions can move to FAILED"
            );
        }

        transaction.setStatus(TransactionStatus.FAILED);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        return new TransactionResponse(savedTransaction);
    }

    public void deleteTransaction(
            Long id,
            String email
    ) {
        Transaction transaction = getUserTransaction(id, email);

        transactionRepository.delete(transaction);
    }

    private Transaction getUserTransaction(
            Long id,
            String email
    ) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Transaction transaction = transactionRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found")
                );

        if (!transaction.getUserId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not authorized to access this transaction"
            );
        }

        return transaction;
    }
}