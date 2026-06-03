package com.ensolution.ems.client_management.presentation.request;

public record CreateCompanyRequest(
	String name,
	String bizNumber,
	String representative,
	String address,
	
	String manager,
	String email,
	String tel
) {}