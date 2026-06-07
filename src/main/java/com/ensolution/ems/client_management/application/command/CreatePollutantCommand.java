package com.ensolution.ems.client_management.application.command;

public record CreatePollutantCommand(
	String nameKr,
	String nameEn
) {}
