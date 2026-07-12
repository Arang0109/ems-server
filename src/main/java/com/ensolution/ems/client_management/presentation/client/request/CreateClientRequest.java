package com.ensolution.ems.client_management.presentation.client.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateClientRequest(
	@NotBlank(message = "의뢰기관명은 필수 입력값입니다.")
	String name,

	@Pattern(regexp = "^$|^\\d{10}$", message = "10자리의 사업자번호를 입력해주세요.")
	String bizNumber,
	String representative,
	String zipcode,
	String roadAddress,
	String address,

	String manager,

	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,
	String tel
) {}
