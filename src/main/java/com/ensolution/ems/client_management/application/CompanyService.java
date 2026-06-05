package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.CreateCompanyCommand;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.port.CompanyRepository;
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
	
	public Company getCompany(Long companyId) { return companyRepository.findById(companyId); }
	
	public List<Company> getCompanyList() {
		return companyRepository.findAll();
	}
}
