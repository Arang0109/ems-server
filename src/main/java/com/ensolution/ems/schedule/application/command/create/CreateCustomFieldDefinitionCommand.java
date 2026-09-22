package com.ensolution.ems.schedule.application.command.create;

/** @param sortOrder null이면 목록 맨 뒤({@code max+10})에 붙는다 */
public record CreateCustomFieldDefinitionCommand(
	Long tenantId,
	String key,
	String label,
	Integer sortOrder
) {}
