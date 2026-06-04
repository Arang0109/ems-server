package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.JpaStackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaStackRepository extends JpaRepository<JpaStackEntity, Long> {
    List<JpaStackEntity> findByWorkplaceId(Long workplaceId);
}
