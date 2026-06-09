package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.create.CreateCompanyCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateCompanyCommand;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.port.CompanyRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {
	
	private final CompanyRepository companyRepository;
	
	public Company createCompany(CreateCompanyCommand command) {
		if (companyRepository.existsByName(command.name())) { throw new CustomException(ErrorCode.CONFLICT); }
		
		Company newCompany = Company.register(
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.address(),
			command.manager(),
			command.email(),
			command.tel()
		);
		return companyRepository.save(newCompany);
	}
	
	public Company updateCompany(Long companyId, UpdateCompanyCommand command) {
		Company company = companyRepository.findById(companyId);
		
		Company savedCompany = company.update(
			companyId,
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.address(),
			command.manager(),
			command.email(),
			command.tel()
		);
		
		return companyRepository.save(savedCompany);
	}

	public Company getCompany(Long companyId) { return companyRepository.findById(companyId); }
	public List<Company> getCompanyList() { return companyRepository.findAll(); }
	public void deleteCompany(Long companyId) { companyRepository.deleteById(companyId); }
}
