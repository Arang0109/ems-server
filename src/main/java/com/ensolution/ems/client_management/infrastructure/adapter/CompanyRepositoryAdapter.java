package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.application.port.out.CompanyRepository;
import com.ensolution.ems.client_management.infrastructure.entity.CompanyEntity;
import com.ensolution.ems.client_management.infrastructure.repository.CompanyJpaRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.CompanyEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {
	
	private final CompanyJpaRepository jpaCompanyRepository;
	private final CompanyEntityMapper mapper;
	
	@Override
	public Company save(Company company) {
		if (company.getId() != null) {
			CompanyEntity existing = jpaCompanyRepository.findById(company.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			CompanyEntity updated = mapper.toEntity(company).toBuilder()
				.workplaces(existing.getWorkplaces())
				.build();
			return mapper.toDomain(jpaCompanyRepository.save(updated));
		}
		return mapper.toDomain(jpaCompanyRepository.save(mapper.toEntity(company)));
	}
	
	@Override
	public Company findById(Long companyId) {
		CompanyEntity company = jpaCompanyRepository.findById(companyId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		return mapper.toDomain(company);
	}
	
	@Override
	public List<Company> findAll() {
		List<CompanyEntity> companies = jpaCompanyRepository.findAll();
		return mapper.toDomainList(companies);
	}
	
	@Override
	public void deleteById(Long companyId) { jpaCompanyRepository.deleteById(companyId); }
	
	@Override
	public boolean existsByName(String name) { return jpaCompanyRepository.existsByName(name); }
}