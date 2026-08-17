package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객사에게 <b>지원하는 측정물질 가이드</b>. 모든 tenant가 공통으로 참조하는 전역 데이터이므로
 * {@code tenantId}를 갖지 않는다.
 *
 * <p>카탈로그는 "무엇을 쓸 수 있는가"만 정의한다. 표기명(영문)·시험장비·시험방법 같은 <b>고객사 표기값은
 * 소유하지 않는다</b> — 그 값들은 {@link Pollutant}가 직접 관리한다. 여기서 보유하는 {@code nameKr}은
 * 고객사가 물질을 채택할 때 복사해 가는 <b>초기값</b>이며, 이후 카탈로그를 고쳐도 이미 채택한 고객사에는
 * 반영되지 않는다. 반대로 {@code field}·{@code method}·{@code phase}는 카탈로그가 단일 진실 소스이며
 * 조회 시 조인으로 전파되므로 법령 개정이 즉시 반영된다.
 *
 * <p>{@code code}는 클라이언트가 특정 물질을 판별하는 불변 키다(예: {@code NOX}). 한번 부여하면 변경하지 않는다 —
 * 측정계획 스냅샷에 영구 보관되고 클라이언트 분기 로직이 이 값에 의존한다.
 *
 * <p>유일 범위는 <b>측정분야 안</b>이다. 같은 물질이라도 대기와 수질은 배출허용기준·공정시험법이 다른 별개
 * 항목이므로, 대기 납과 수질 납이 모두 {@code PB}로 존재한다. 따라서 code만으로 물질을 특정할 수 없고,
 * 다른 계층에서 카탈로그를 지목할 때는 {@code id}를 쓴다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PollutantCatalog {
	private Long id;
	private String code;
	private MeasurementField field;
	/** 가이드 표준 국문명. 고객사가 채택할 때 복사해 가는 초기값이다. */
	private String nameKr;
	private MeasurementMethod method;
	private PollutantPhase phase;
	private Integer sortOrder;
	private boolean active;

	public static PollutantCatalog register(
		String code,
		MeasurementField field,
		String nameKr,
		MeasurementMethod method,
		PollutantPhase phase,
		Integer sortOrder
	) {
		return PollutantCatalog.builder()
			.code(code)
			.field(field)
			.nameKr(nameKr)
			.method(method)
			.phase(phase)
			.sortOrder(sortOrder)
			.active(true)
			.build();
	}

	/**
	 * 전달되지 않은(null·blank) 필드는 기존 값을 유지한다.
	 * {@code code}는 대상에서 제외한다 — 프론트 분기와 기존 스냅샷을 깨뜨리므로 변경을 허용하지 않는다.
	 *
	 * <p>{@code nameKr} 수정은 <b>앞으로 채택할</b> 고객사에만 영향을 준다. 이미 채택한 고객사는
	 * 자신의 표기명을 직접 보유하므로 값이 바뀌지 않는다.
	 */
	public PollutantCatalog update(
		MeasurementField field,
		String nameKr,
		MeasurementMethod method,
		PollutantPhase phase,
		Integer sortOrder
	) {
		return this.toBuilder()
			.field(keep(field, this.field))
			.nameKr(keep(nameKr, this.nameKr))
			.method(keep(method, this.method))
			.phase(keep(phase, this.phase))
			.sortOrder(keep(sortOrder, this.sortOrder))
			.build();
	}

	/** 폐지된 법정 물질을 목록에서 감춘다. 이미 채택해 쓰고 있는 tenant의 데이터는 그대로 유지된다. */
	public PollutantCatalog deactivate() {
		return this.toBuilder().active(false).build();
	}

	public PollutantCatalog activate() {
		return this.toBuilder().active(true).build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}

	private static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}
}
