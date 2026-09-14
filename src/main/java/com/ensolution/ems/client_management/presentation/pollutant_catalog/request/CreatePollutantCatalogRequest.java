package com.ensolution.ems.client_management.presentation.pollutant_catalog.request;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * @param code 측정분야 안에서 물질을 가리키는 불변 키. 대문자·숫자·밑줄만 쓰며 화학식이나 원소기호를
 *             우선한다(예: {@code NOX}, {@code PB}, {@code F}). 등록 후에는 변경할 수 없다.
 *             대기 납과 수질 납처럼 분야가 다르면 같은 code를 쓸 수 있다
 * @param nameKr 가이드 표준 국문명. 고객사가 채택할 때 복사해 가는 초기값이다.
 *               영문명·시험장비·시험방법·측정방법은 고객사가 직접 관리하므로 카탈로그에 두지 않는다
 * @param mode   측정방식 분류(현장측정·먼지·중금속·수은·가스상 채취). 회사와 무관한 전역 사실이라 카탈로그가 갖는다.
 *               두 방식에 걸치는 항목(비소화합물)은 주 방식 하나를 준다
 */
public record CreatePollutantCatalogRequest(
	@NotBlank
	@Pattern(regexp = "^[A-Z][A-Z0-9_]{0,29}$", message = "코드는 대문자로 시작하는 1~30자의 대문자·숫자·밑줄 조합이어야 합니다.")
	String code,
	@NotNull
	MeasurementField field,
	@NotBlank
	String nameKr,
	PollutantPhase phase,
	MeasurementMode mode,
	Integer sortOrder
) {}
