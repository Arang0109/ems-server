package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.common.enums.SampleGrouping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 고객사가 {@link PollutantCatalog}(지원 물질 가이드)에서 <b>채택한</b> 측정물질.
 * 가이드에 없는 물질은 만들 수 없으므로 {@code catalogId}는 항상 존재한다.
 *
 * <p>필드는 세 부류로 나뉜다.
 * <ul>
 *   <li><b>고객사 소유값</b> — {@code methodId}, {@code samplingMinutes}, {@code suctionFlowRate}, {@code nameKr}, {@code nameEn},
 *       {@code equipment}, {@code testMethod}.
 *       DB 컬럼이며 고객사가 직접 입력·관리한다. 채택 시 {@code nameKr}만 카탈로그 값을 복사해 두고
 *       나머지는 비워 둔다. 이후 카탈로그를 고쳐도 이 값들은 바뀌지 않는다.
 *       {@code methodId}는 채택 시 고객사가 정한다 — 같은 카탈로그 항목이라도 업체마다 측정방법이 다를 수 있기
 *       때문이다(예: 이황화메틸은 테드라백·카트리지 둘 다 쓰인다). 카탈로그에는 기본값도 없다.
 *       {@code samplingMinutes}는 <b>항목별 채취시간 오버라이드</b>다 — 흡수액처럼 항목마다 따로 잡는 방법은
 *       물질마다 흡인 시간이 다를 수 있어 방법의 기본값을 덮어쓴다. 한 병으로 함께 잡는({@code MERGED}) 방법의
 *       항목에는 둘 수 없다(항목마다 시간이 다르면 한 병이 아니다). {@code suctionFlowRate}도 같은 구조의
 *       <b>항목별 흡인유량 오버라이드</b>다 — 흡수액은 물질마다 유량이 정해져 있지만, 통칭 시료({@code VOCs}·{@code VOCs-T})는
 *       한 병을 한 펌프로 잡으므로 방법이 정한다.</li>
 *   <li><b>카탈로그 투영값</b> — {@code code}, {@code field}, {@code phase}, {@code mode}(측정방식 분류).
 *       DB 컬럼이 아니라 조회 시 카탈로그에서 조인해 채우는 읽기 전용 값이다. 카탈로그가 단일 진실 소스이므로
 *       법령 개정이 즉시 반영된다.</li>
 *   <li><b>측정방법 투영값</b> — {@code methodName}, {@code sampleGrouping}, {@code mergedSampleName},
 *       {@code methodSamplingMinutes}, {@code methodSuctionFlowRate}. 역시 DB 컬럼이 아니라 {@link MeasurementMethod}에서 조인해 채우는 읽기 전용
 *       값이다. 채취 단위와 기본 채취시간은 물질이 아니라 측정방법에 종속되는 값이라 여기 컬럼으로 두지 않는다 —
 *       두면 카트리지 항목마다 같은 값을 반복 저장하고 바꿀 때마다 동기화해야 한다. 측정방법을 고치면
 *       그 방법을 쓰는 항목 전부에 즉시 반영된다.</li>
 * </ul>
 * 두 투영은 모두 {@code PollutantEntityMapper}가 담당한다.
 * 이 항목에 실제로 적용되는 채취시간·흡인유량은 {@link #getEffectiveSamplingMinutes()}·{@link #getEffectiveSuctionFlowRate()}가 정한다.
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
	private MeasurementMode mode;

	// --- 고객사 소유값 (DB 컬럼) ---
	/**
	 * 이 고객사가 이 물질에 쓰는 측정방법. 채택 시 정하고 이후 바꿀 수 있다.
	 * 측정방법을 애그리거트로 승격하기 전 백필되지 못한 레거시 행만 null이다.
	 */
	private Long methodId;
	/** 항목별 채취시간 오버라이드(분). null이면 측정방법의 기본값을 따른다. */
	private Integer samplingMinutes;
	/** 항목별 흡인유량 오버라이드(L/min). null이면 측정방법의 기본값을 따른다. */
	private BigDecimal suctionFlowRate;
	private String nameKr;
	private String nameEn;
	private String equipment;
	private String testMethod;

	// --- 측정방법 투영값 (DB 컬럼 아님) ---
	private String methodName;
	private SampleGrouping sampleGrouping;
	private String mergedSampleName;
	private Integer methodSamplingMinutes;
	private BigDecimal methodSuctionFlowRate;

	/**
	 * 이 항목에 적용되는 표준 채취시간(분).
	 * 한 병으로 함께 잡는({@link SampleGrouping#MERGED}) 방법은 항목마다 시간이 다를 수 없으므로 방법 값만 본다 —
	 * 방법을 나중에 MERGED로 바꿔 남게 된 오버라이드는 그래서 무시된다. 그 외에는 오버라이드가 있으면 그것을,
	 * 없으면 방법의 기본값을 쓴다. 측정방법이 정해지지 않은 레거시 행은 오버라이드만 볼 수 있다.
	 */
	public Integer getEffectiveSamplingMinutes() {
		if (sampleGrouping == SampleGrouping.MERGED) return methodSamplingMinutes;
		return samplingMinutes != null ? samplingMinutes : methodSamplingMinutes;
	}

	/**
	 * 이 항목에 적용되는 표준 흡인유량(L/min). 규칙은 {@link #getEffectiveSamplingMinutes()}와 같다 —
	 * 한 병으로 함께 잡는 방법은 유량도 하나이므로 방법 값만 보고, 그 외에는 오버라이드가 있으면 그것을 쓴다.
	 */
	public BigDecimal getEffectiveSuctionFlowRate() {
		if (sampleGrouping == SampleGrouping.MERGED) return methodSuctionFlowRate;
		return suctionFlowRate != null ? suctionFlowRate : methodSuctionFlowRate;
	}

	/**
	 * 가이드 항목을 채택한다. {@code nameKr}을 주지 않으면 카탈로그의 표준 국문명을 복사한다 —
	 * 이후에는 고객사 소유값이므로 카탈로그가 바뀌어도 따라가지 않는다.
	 * {@code methodId}는 고객사가 정하는 값이라 카탈로그에서 가져오지 않는다.
	 *
	 * <p>카탈로그 투영값까지 채워 돌려주므로 생성 응답에 재조회가 필요 없다. 측정방법 투영값은
	 * 저장 시 어댑터가 조인해 채운다.
	 */
	public static Pollutant register(
		Long tenantId,
		PollutantCatalog catalog,
		Long methodId,
		Integer samplingMinutes,
		BigDecimal suctionFlowRate,
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
			.mode(catalog.getMode())
			.methodId(methodId)
			.samplingMinutes(samplingMinutes)
			.suctionFlowRate(suctionFlowRate)
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
	 * {@code methodId}는 고객사 소유값이므로 바꿀 수 있다. 측정방법 투영값은 저장 후 어댑터가 다시 채우므로
	 * 여기서 손대지 않는다.
	 *
	 * <p>{@code samplingMinutes}·{@code suctionFlowRate}만 <b>전달값을 그대로 채택</b>한다. "오버라이드 없음(방법 기본값으로 되돌림)"이
	 * 유효한 값이라 null을 "유지"로 읽으면 한번 넣은 오버라이드를 걷어낼 방법이 없어진다
	 * ({@code StackPollutant#update}의 허용기준과 같은 판단이다). 수정 폼이 자기 필드 전부를 보내는 것이 전제다.
	 */
	public Pollutant update(
		Long methodId,
		Integer samplingMinutes,
		BigDecimal suctionFlowRate,
		String nameKr,
		String nameEn,
		String equipment,
		String testMethod
	) {
		return this.toBuilder()
			.methodId(keep(methodId, this.methodId))
			.samplingMinutes(samplingMinutes)
			.suctionFlowRate(suctionFlowRate)
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
