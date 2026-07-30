package com.ensolution.ems.client_management.presentation.client.response;

public record ClientResponse(
	Long id,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,

	String manager,
	String email,
	String tel
) {}
