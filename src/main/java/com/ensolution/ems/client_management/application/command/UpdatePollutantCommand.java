package com.ensolution.ems.client_management.application.command;

public record UpdatePollutantCommand(
	String nameKr,
	String nameEn
) {}
