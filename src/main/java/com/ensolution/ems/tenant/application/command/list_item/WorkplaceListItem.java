package com.ensolution.ems.tenant.application.command.list_item;

public record WorkplaceListItem(
	Long id,
	Long clientId,
	String clientName,
	String name,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode
) {}
