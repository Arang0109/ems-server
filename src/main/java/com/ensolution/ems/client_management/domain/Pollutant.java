package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객사가 {@link PollutantCatalog}(지원 물질 가이드)에서 <b>채택한</b> 측정물질.
 * 가이드에 없는 물질은 만들 수 없으므로 {@code catalogId}는 항상 존재한다.
 *
 * <p>필드는 두 부류로 나뉜다.
 * <ul>
 *   <li><b>고객사 소유값</b> — {@code method}, {@code nameKr}, {@code nameEn}, {@code equipment}, {@code testMethod}.
 *       DB 컬럼이며 고객사가 직접 입력·관리한다. 채택 시 {@code nameKr}만 카탈로그 값을 복사해 두고
 *       나머지는 비워 둔다. 이후 카탈로그를 고쳐도 이 값들은 바뀌지 않는다.
 *       {@code method}는 채택 시 고객사가 정한다 — 같은 카탈로그 항목이라도 업체마다 측정방법이 다를 수 있기
 *       때문이다(예: 이황화메틸은 테드라백·카트리지 둘 다 쓰인다). 카탈로그에는 기본값도 없다.</li>
 *   <li><b>카탈로그 투영값</b> — {@code code}, {@code field}, {@code phase}.
 *       DB 컬럼이 아니라 조회 시 카탈로그에서 조인해 채우는 읽기 전용 값이다. 카탈로그가 단일 진실 소스이므로
 *       법령 개정이 즉시 반영된다. 투영은 {@code PollutantEntityMapper}가 담당한다.</li>
 * </ul>
 *
 * <p>{@code code}는 측정분야 안에서만 유일하므로 분야가 다르면 같은 값이 나올 수 있다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Pollutant {
	private Long id;
	private Long tenantId;
	/** 채택한 가이드 항목. 고객사는 가이드 밖의 물질을 만들 수 없으므로 항상 값이 있다. */
	private Long catalogId;

	// --- 카탈로그 투영값 (DB 컬럼 아님) ---
	private String code;
	private MeasurementField field;
	private PollutantPhase phase;

	// --- 고객사 소유값 (DB 컬럼) ---
	/** 이 고객사가 이 물질에 쓰는 측정방법. 채택 시 정하고 이후 바꿀 수 있다. */
	private MeasurementMethod method;
	private String nameKr;
	private String nameEn;
	private String equipment;
	private String testMethod;

	/**
	 * 가이드 항목을 채택한다. {@code nameKr}을 주지 않으면 카탈로그의 표준 국문명을 복사한다 —
	 * 이후에는 고객사 소유값이므로 카탈로그가 바뀌어도 따라가지 않는다.
	 * {@code method}는 고객사가 정하는 값이라 카탈로그에서 가져오지 않는다.
	 *
	 * <p>카탈로그 투영값까지 채워 돌려주므로 생성 응답에 재조회가 필요 없다.
	 */
	public static Pollutant register(
		Long tenantId,
		PollutantCatalog catalog,
		MeasurementMethod method,
		String nameKr,
		String nameEn,
		String equipment,
		String testMethod
	) {
		return Pollutant.builder()
			.tenantId(tenantId)
			.catalogId(catalog.getId())
			.code(catalog.getCode())
			.field(catalog.getField())
			.phase(catalog.getPhase())
			.method(method)
			.nameKr(keep(nameKr, catalog.getNameKr()))
			.nameEn(nameEn)
			.equipment(equipment)
			.testMethod(testMethod)
			.build();
	}

	/**
	 * 고객사 소유값만 수정한다. 전달되지 않은(null·blank) 필드는 기존 값을 유지한다.
	 *
	 * <p>{@code catalogId}는 대상이 아니다 — 어떤 물질인지가 바뀌면 다른 물질이지 수정이 아니다.
	 * {@code field}·{@code phase}도 카탈로그 소유이므로 여기서 바꿀 수 없다.
	 * {@code method}는 고객사 소유값이므로 바꿀 수 있다.
	 */
	public Pollutant update(
		MeasurementMethod method,
		String nameKr,
		String nameEn,
		String equipment,
		String testMethod
	) {
		return this.toBuilder()
			.method(keep(method, this.method))
			.nameKr(keep(nameKr, this.nameKr))
			.nameEn(keep(nameEn, this.nameEn))
			.equipment(keep(equipment, this.equipment))
			.testMethod(keep(testMethod, this.testMethod))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}

	private static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}
}
