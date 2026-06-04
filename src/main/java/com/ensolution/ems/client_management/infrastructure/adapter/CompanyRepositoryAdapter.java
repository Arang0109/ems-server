package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.port.CompanyRepository;
import com.ensolution.ems.client_management.infrastructure.entity.JpaCompanyEntity;
import com.ensolution.ems.client_management.infrastructure.repository.JpaCompanyRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.CompanyDomainEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {
	
	private final JpaCompanyRepository jpaCompanyRepository;
	private final CompanyDomainEntityMapper mapper;
	
	@Override
	public Company save(Company company) {
		JpaCompanyEntity savedCompany = jpaCompanyRepository.save(mapper.toEntity(company));
		return mapper.toDomain(savedCompany);
	}
	
	@Override
	public List<Company> findAll() {
		List<JpaCompanyEntity> companies = jpaCompanyRepository.findAll();
		return mapper.toDomainList(companies);
	}
}