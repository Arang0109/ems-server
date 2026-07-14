package com.ensolution.ems.auth.infrastructure.repository;

import com.ensolution.ems.auth.infrastructure.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

	Optional<RoleEntity> findByName(String name);

	boolean existsByName(String name);
}
