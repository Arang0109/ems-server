package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateCompanyCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateCompanyCommand;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.application.port.out.CompanyRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyCommandService {

	private final CompanyRepository companyRepository;

	public Company createCompany(CreateCompanyCommand command) {
		if (companyRepository.existsByName(command.name())) { throw new CustomException(ErrorCode.CONFLICT); }

		Company newCompany = Company.register(
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.zipcode(),
			command.roadAddress(),
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
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.zipcode(),
			command.roadAddress(),
			command.address(),
			command.manager(),
			command.email(),
			command.tel()
		);

		return companyRepository.save(savedCompany);
	}

	public void deleteCompany(Long companyId) { companyRepository.deleteById(companyId); }
}
