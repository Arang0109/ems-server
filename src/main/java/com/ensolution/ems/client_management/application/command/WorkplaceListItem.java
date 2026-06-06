package com.ensolution.ems.client_management.application.command;

public record WorkplaceListItem(
	Long id,
	Long companyId,
	String companyName,
	String name,
	String bizNumber,
	String address
) {}