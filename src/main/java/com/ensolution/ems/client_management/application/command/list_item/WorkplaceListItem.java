package com.ensolution.ems.client_management.application.command.list_item;

public record WorkplaceListItem(
	Long id,
	Long companyId,
	String companyName,
	String name,
	String bizNumber,
	String zipcode,
	String roadAddress,
	String address
) {}