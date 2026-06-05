package com.ensolution.ems.client_management.domain.port;

import com.ensolution.ems.client_management.domain.Company;

import java.util.List;

public interface CompanyRepository {
	Company save(Company company);
	Company findById(Long companyId);
	List<Company> findAll();
}
