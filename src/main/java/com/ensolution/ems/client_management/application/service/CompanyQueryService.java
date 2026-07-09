package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.application.port.out.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyQueryService {

	private final CompanyRepository companyRepository;

	public Company getCompany(Long companyId) { return companyRepository.findById(companyId); }

	public List<Company> getCompanyList() { return companyRepository.findAll(); }
}
