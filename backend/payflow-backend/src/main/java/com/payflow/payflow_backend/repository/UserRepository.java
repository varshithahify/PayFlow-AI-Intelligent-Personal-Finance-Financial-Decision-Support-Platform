package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
User findByEmail(String email);

List<User> findAllByOrgId(Long orgId);

Optional<User> findByEmailAndOrgId(String email, Long orgId);


}
