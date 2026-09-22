package com.ensolution.ems.schedule.presentation.custom_field.request;

import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param key       템플릿이 {@code ${custom.<key>}}로 읽는 이름. 영문자·숫자·밑줄, 영문자 또는 밑줄로 시작. 등록 후 변경 불가
 * @param label     화면에 보이는 이름
 * @param sortOrder 비우면 목록 맨 뒤에 붙습니다
 */
public record CreateCustomFieldDefinitionRequest(
	@NotBlank
	@Size(max = CustomFieldDefinition.KEY_MAX_LENGTH)
	@Pattern(regexp = CustomFieldDefinition.KEY_REGEX)
	String key,
	@NotBlank
	@Size(max = CustomFieldDefinition.LABEL_MAX_LENGTH)
	String label,
	Integer sortOrder
) {}
