package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.port.CompanyRepository;
import com.ensolution.ems.client_management.infrastructure.entity.JpaCompanyEntity;
import com.ensolution.ems.client_management.infrastructure.repository.JpaCompanyRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.CompanyDomainEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
	public Company findById(Long companyId) {
		JpaCompanyEntity company = jpaCompanyRepository.findById(companyId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "해당 ID로 등록된 회사를 찾을 수 업습니다."));
		return mapper.toDomain(company);
	}
	
	@Override
	public List<Company> findAll() {
		List<JpaCompanyEntity> companies = jpaCompanyRepository.findAll();
		return mapper.toDomainList(companies);
	}
	
	@Override
	public void deleteById(Long companyId) { jpaCompanyRepository.deleteById(companyId); }
}