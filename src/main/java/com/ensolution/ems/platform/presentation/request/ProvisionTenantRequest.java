package com.ensolution.ems.platform.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 고객사 발급 요청. 테넌트 정보 + 초기 관리자 계정.
 */
public record ProvisionTenantRequest(
	@NotBlank(message = "고객사명은 필수 입력값입니다.")
	String name,
	@Pattern(regexp = "^\\d{10}$", message = "10자리의 사업자번호를 입력해주세요.")
	String bizNumber,
	@NotBlank(message = "구독 플랜은 필수 입력값입니다.")
	String subscriptionPlan,
	@NotNull(message = "초기 관리자 정보는 필수입니다.")
	@Valid
	TenantAdminRequest admin
) {}
