package com.ensolution.ems.schedule.domain.analysis;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 한 측정계획에서 한 측정항목을 실험실 분석한 결과(실험분석정보). MongoDB에 측정항목 하나당 문서 하나로 남으며,
 * 측정 시트(실측·계산값)와는 별개의 애그리거트다 — 현장 측정이 끝난 뒤 실험실에서 따로 입력되고,
 * 시트의 동시 편집·낙관적 락 경로에 얽히지 않아야 하기 때문이다.
 *
 * <p>{@code allowance}·{@code oxygenApplicable}은 측정계획 스냅샷의 측정항목
 * ({@link SamplingItemSnapshot} = 측정 시점의 stack_pollutant 원장 사본)에서 복사해 고정한다.
 * 원장의 허용기준이 나중에 개정되어도 과거 회차의 초과 판정이 흔들리면 안 되기 때문이며,
 * 그래서 실험실 입력값 수정 경로({@link #update})에서는 이 두 값을 바꾸지 않는다.
 *
 * <p><b>이 애그리거트는 두 화면이 필드를 나눠 소유한다.</b> 실험·분석 탭은 실험실 입력값
 * ({@code analysisValue}·{@code unit}·{@code analysisMethod}·{@code analysisEquipment})만,
 * 성적서 탭은 채취시간({@code samplingStartedAt}·{@code samplingEndedAt})만 쓴다.
 * 그래서 수정 경로도 {@link #applyAnalysisResult}와 {@link #applySamplingTime}으로 갈라 두었다 —
 * 두 탭을 동시에 열어 두어도 서로의 입력을 덮어쓰지 않아야 하기 때문이다.
 *
 * <p>두 탭 모두 <b>측정물질(pollutantId)을 키로 하는 일괄 upsert</b>로 저장한다. 문서의 대리키
 * ({@code id})로 신규·기존을 판별하게 두면, 한 탭이 문서를 만든 사실을 다른 탭이 모른 채 등록을
 * 시도해 충돌한다 — 실제 불변식은 "한 계획의 한 측정항목 = 문서 하나"이므로 자연키로 쓰는 편이 맞다.
 *
 * <p>채취시간을 현장 채취 시트에서 파생하지 않고 성적서 탭에서 직접 받는 이유는 두 문서의
 * 항목 단위가 다르기 때문이다. 채취기록지는 알데히드류를 {@code VOCs}로 통칭해 시료 한 건으로
 * 적지만 성적서는 포름알데히드·아세트알데히드를 각각 쓴다(시료 1건 ↔ 항목 N건). 그 통칭 규칙은
 * 업체마다 달라 서버가 고정할 수 없어, 자동으로 옮기면 조용히 틀린 시각이 성적서에 찍힌다.
 *
 * <p>다만 그 회차의 측정항목 스냅샷 자체가 잘못 복사되어 정정될 때는
 * {@link #syncJudgementBasis}로 따라가야 한다 — 같은 회차 안에서 판정 근거가 둘로 갈라지면
 * 어느 쪽이 성적서의 기준인지 알 수 없게 된다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AnalysisRecord {

	private String id;                  // Mongo _id
	private Long tenantId;
	private Long scheduleId;            // 소속 측정계획

	// --- 측정항목 식별 (스냅샷 사본) ---
	private Long stackPollutantId;      // 원장 연결키
	private Long pollutantId;           // 측정물질. 계획 안에서 분석 기록의 유일성 축
	private String pollutantName;

	// --- 원장에서 고정된 판정 근거 ---
	private BigDecimal allowance;       // 허용기준치
	private boolean oxygenApplicable;   // 기준산소농도 보정 적용 여부

	// --- 실험실 입력값 ---
	private BigDecimal analysisValue;   // 측정분석값
	private String unit;                // 측정단위
	private String analysisMethod;      // 측정분석방법
	private String analysisEquipment;   // 분석장비

	// --- 성적서 입력값 ---
	private LocalTime samplingStartedAt; // 채취 시작시각
	private LocalTime samplingEndedAt;   // 채취 종료시각

	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	/**
	 * 측정계획의 측정항목 하나에 대한 분석 결과를 등록한다.
	 * 허용기준치·산소보정 적용 여부·측정물질명은 스냅샷 항목에서 복사하므로 원장을 다시 읽지 않는다.
	 */
	public static AnalysisRecord register(
		Long tenantId,
		Long scheduleId,
		SamplingItemSnapshot item,
		BigDecimal analysisValue,
		String unit,
		String analysisMethod,
		String analysisEquipment
	) {
		return AnalysisRecord.builder()
			.tenantId(tenantId)
			.scheduleId(scheduleId)
			.stackPollutantId(item.stackPollutantId())
			.pollutantId(item.pollutantId())
			.pollutantName(item.nameKr())
			.allowance(item.allowance())
			.oxygenApplicable(item.oxygenApplicable())
			.analysisValue(analysisValue)
			.unit(unit)
			.analysisMethod(analysisMethod)
			.analysisEquipment(analysisEquipment)
			.build();
	}

	/**
	 * 실험실 입력값만 수정한다. 전달되지 않은(null·blank) 필드는 기존 값을 유지한다.
	 * 허용기준치·산소보정 적용 여부는 측정 시점 원장 값이므로 대상이 아니며,
	 * 어느 측정항목의 분석인지가 바뀌면 다른 기록이지 수정이 아니므로 항목 식별자도 바꾸지 않는다.
	 *
	 * <p><b>채취시간은 인자로 받지도, 바꾸지도 않는다.</b> 그것은 성적서 탭이 소유하는 필드이고,
	 * 실험·분석 탭의 저장이 성적서 탭의 입력을 지우면 안 된다({@link #applySamplingTime} 참고).
	 */
	public AnalysisRecord update(
		BigDecimal analysisValue,
		String unit,
		String analysisMethod,
		String analysisEquipment
	) {
		return this.toBuilder()
			.analysisValue(keep(analysisValue, this.analysisValue))
			.unit(keep(unit, this.unit))
			.analysisMethod(keep(analysisMethod, this.analysisMethod))
			.analysisEquipment(keep(analysisEquipment, this.analysisEquipment))
			.build();
	}

	/**
	 * 판정 근거(허용기준치·산소보정 적용 여부)를 이 회차의 측정항목 스냅샷에 다시 맞춘다.
	 *
	 * <p>원장 개정을 과거 회차에 소급 반영하는 통로가 <b>아니다</b>. 스냅샷의 측정항목이
	 * 정정될 때만 그 정정을 따라가는 경로이며, 호출자는 정정된 스냅샷 항목을 그대로 넘긴다.
	 * 실험실 입력값({@code analysisValue} 등)은 손대지 않는다 — 기준이 바뀌었을 뿐
	 * 분석 결과가 달라진 것은 아니다.
	 */
	public AnalysisRecord syncJudgementBasis(SamplingItemSnapshot item) {
		return this.toBuilder()
			.allowance(item.allowance())
			.oxygenApplicable(item.oxygenApplicable())
			.build();
	}

	/**
	 * 채취시간만 가진 새 기록을 만든다. 성적서 작성이 실험실 분석보다 먼저 올 수 있으므로,
	 * 분석 결과 없이도 그 항목의 기록이 생겨야 한다.
	 * 판정 근거(허용기준치·산소보정)는 {@link #register}와 같이 스냅샷 항목에서 복사한다 —
	 * 어느 경로로 만들어졌든 한 회차 안에서 판정 근거가 갈라지면 안 된다.
	 */
	public static AnalysisRecord ofSamplingTime(
		Long tenantId,
		Long scheduleId,
		SamplingItemSnapshot item,
		LocalTime samplingStartedAt,
		LocalTime samplingEndedAt
	) {
		return AnalysisRecord.builder()
			.tenantId(tenantId)
			.scheduleId(scheduleId)
			.stackPollutantId(item.stackPollutantId())
			.pollutantId(item.pollutantId())
			.pollutantName(item.nameKr())
			.allowance(item.allowance())
			.oxygenApplicable(item.oxygenApplicable())
			.samplingStartedAt(samplingStartedAt)
			.samplingEndedAt(samplingEndedAt)
			.build();
	}

	/**
	 * 실험실 입력값만 교체한다. 채취시간은 건드리지 않는다 — 성적서 탭이 소유하는 필드다.
	 *
	 * <p>{@link #update}와 달리 <b>전달값을 그대로 채택한다.</b> 실험·분석 탭이 항목 표 전체를 보내는
	 * 일괄 저장용이라 빈 칸은 "미전달"이 아니라 "지웠다"는 뜻이며, 그래야 잘못 넣은 값을 다시 비울 수 있다
	 * ({@link #applySamplingTime}과 같은 판단이다).
	 */
	public AnalysisRecord applyAnalysisResult(
		BigDecimal analysisValue,
		String unit,
		String analysisMethod,
		String analysisEquipment
	) {
		return this.toBuilder()
			.analysisValue(analysisValue)
			.unit(unit)
			.analysisMethod(analysisMethod)
			.analysisEquipment(analysisEquipment)
			.build();
	}

	/**
	 * 채취시간만 교체한다. 실험실 입력값은 건드리지 않는다 — 성적서 탭이 소유하는 필드만 쓴다.
	 *
	 * <p><b>전달값을 그대로 채택한다.</b> {@code update}처럼 null을 "미전달"로 읽으면 한번 채운 뒤로는
	 * 잘못 넣은 시각을 비울 방법이 없어진다({@code SamplingItemSnapshot#applyCondition}의
	 * {@code allowance}와 같은 판단이다). 성적서 탭이 항목 표 전체를 보내는 일괄 저장이므로
	 * 빈 칸은 "지웠다"는 뜻으로 읽는 편이 맞다.
	 *
	 * <p>시작·종료의 순서는 검증하지 않는다 — 자정을 넘겨 채취하는 회차(23:00→01:00)가 실제로 있어
	 * 시작이 종료보다 늦은 것이 정상인 경우가 있다.
	 */
	public AnalysisRecord applySamplingTime(LocalTime samplingStartedAt, LocalTime samplingEndedAt) {
		return this.toBuilder()
			.samplingStartedAt(samplingStartedAt)
			.samplingEndedAt(samplingEndedAt)
			.build();
	}

	/** 경로의 측정계획에 속한 기록인지 확인한다. 다른 계획의 기록을 id만으로 조작하지 못하게 막는다. */
	public void requireBelongsTo(Long scheduleId) {
		if (this.scheduleId == null || !this.scheduleId.equals(scheduleId)) {
			throw new CustomException(ErrorCode.SCHEDULE_ANALYSIS_NOT_FOUND);
		}
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}

	private static BigDecimal keep(BigDecimal value, BigDecimal original) {
		return value == null ? original : value;
	}
}
