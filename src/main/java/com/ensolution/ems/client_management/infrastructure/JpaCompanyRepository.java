package com.ensolution.ems.client_management.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCompanyRepository extends JpaRepository<JpaCompanyEntity, Long> {
}