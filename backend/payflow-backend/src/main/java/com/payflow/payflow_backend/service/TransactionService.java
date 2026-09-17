package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

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
                "CREATED",
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

        return new TransactionResponse(transaction);
    }

    public void deleteTransaction(
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
                    "You are not authorized to delete this transaction"
            );
        }

        transactionRepository.delete(transaction);
    }
}