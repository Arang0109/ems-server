package com.ensolution.ems.client_management.presentation.company.response;

public record CompanyResponse(
	Long id,
	String name,
	String bizNumber,
	String representative,
	String zipcode,
	String roadAddress,
	String address,
	
	String manager,
	String email,
	String tel
) {}