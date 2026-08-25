package com.ensolution.ems.auth.infrastructure.repository;

import com.ensolution.ems.auth.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
	List<UserEntity> findAllByTenantId(Long tenantId);

	Optional<UserEntity> findByUserIdAndTenantId(Long userId, Long tenantId);

	Optional<UserEntity> findByUsername(String username);

	boolean existsByUsername(String username);

	long deleteByUserIdAndTenantId(Long userId, Long tenantId);
}