package com.ensolution.ems.client_management.presentation.client.response;

public record ClientResponse(
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
