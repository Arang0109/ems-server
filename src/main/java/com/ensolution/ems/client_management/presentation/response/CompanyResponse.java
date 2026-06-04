package com.ensolution.ems.client_management.presentation.response;

public record CompanyResponse(
	Long id,
	String name,
	String bizNumber,
	String representative,
	String address,
	
	String manager,
	String email,
	String tel
) {}