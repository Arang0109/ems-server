package com.ensolution.ems.auth.infrastructure.repository;

import com.ensolution.ems.auth.infrastructure.entity.PrivilegeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivilegeJpaRepository extends JpaRepository<PrivilegeEntity, Long> {

	Optional<PrivilegeEntity> findByName(String name);

	boolean existsByName(String name);
}
