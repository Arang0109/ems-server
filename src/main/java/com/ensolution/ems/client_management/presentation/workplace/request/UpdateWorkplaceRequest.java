package com.ensolution.ems.client_management.presentation.workplace.request;

import com.ensolution.ems.global.common.enums.Grade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateWorkplaceRequest(
	@NotBlank(message = "사업장명은 필수 입력값입니다.")
	String name,
	String roadAddress,
	String detailAddress,
	String zipcode,
	@Pattern(regexp = "^\\d{10}$", message = "10자리의 사업자번호를 입력해주세요.")
	String bizNumber,
	String facilityManager,
	String samplingWitness,
	Grade grade
) {}
