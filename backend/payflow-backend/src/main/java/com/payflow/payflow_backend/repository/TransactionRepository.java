package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}