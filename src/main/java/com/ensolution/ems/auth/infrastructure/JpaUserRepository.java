package com.ensolution.ems.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Long> {
    
    Optional<JpaUserEntity> findByUsername(String username);
    
    boolean existsByUsername(String username);
}