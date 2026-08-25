package com.ensolution.ems.equipment.presentation.request;

import com.ensolution.ems.equipment.domain.InspectionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * 장비의 검사 종류별 설정.
 * <p>
 * 등록 시에는 여기 담긴 종류가 그대로 초기값이 되고, 수정 시에는 전달한 종류만 부분 갱신된다.
 * 플래그를 {@code Boolean} 으로 받는 이유는 "false로 끄기"와 "미전달"을 구분하기 위해서다.
 */
public record InspectionItemRequest(
	@NotNull(message = "검사 종류는 필수 선택값입니다.")
	InspectionType type,

	Boolean enabled,

	@Positive Integer cycleMonths,
	@PastOrPresent LocalDate lastInspectedAt,

	/// 성적서에 유효기간이 명시된 경우의 만료일. 있으면 주기 계산보다 우선한다.
	LocalDate nextDueDateOverride,

	Boolean notificationEnabled
) {}
