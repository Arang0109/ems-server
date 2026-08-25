package com.ensolution.ems.schedule.infrastructure.entity;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 측정항목별 회차 이력 엔티티. 측정계획 완료 시 항목 수만큼 추가되고, 재개방·취소 시 계획 단위로 삭제된다.
 * 측정계획·측정시설·측정물질은 각각 다른 애그리거트/모듈 소유이므로 FK 없이 plain id 컬럼으로 보관한다
 * ({@link ScheduleEntity}와 동일한 방침).
 *
 * <p>FK가 없으므로 측정시설이나 측정항목이 지워져도 이력은 남는다. 의도된 동작이다 —
 * 이력은 "그때 그런 측정이 있었다"는 사실이고, 이행 현황판은 <i>현재 등록된</i> 항목만 행 축으로 삼으므로
 * 삭제된 항목이 현황판에 유령으로 나타나지도 않는다.
 *
 * <p>조회도 유일성 제약도 {@code stack_pollutant_id}가 아니라 {@code pollutant_id}를 축으로 삼는다.
 * 시설별 측정물질에는 수정 경로가 없어 주기 변경이 곧 삭제 후 재등록이고, 그때 {@code stack_pollutant_id}가
 * 새로 발급되어 이력이 끊기기 때문이다. 물질 자체는 그 조작에도 그대로이므로 안정적인 키다.
 *
 * <p>{@code completed_at}은 도메인이 기록 시점에 값을 정하므로 auditing 리스너를 쓰지 않는다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
	name = "measurement_records",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_measurement_records_schedule_pollutant",
			columnNames = {"schedule_id", "pollutant_id"})
	},
	indexes = {
		@Index(name = "idx_measurement_records_stack_period",
			columnList = "tenant_id, stack_id, period_year, period_index"),
		@Index(name = "idx_measurement_records_stack_pollutant",
			columnList = "tenant_id, stack_id, pollutant_id, sampled_at"),
		@Index(name = "idx_measurement_records_tenant_year",
			columnList = "tenant_id, period_year")
	}
)
public class MeasurementRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "record_id")
	private Long recordId;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "stack_id", nullable = false)
	private Long stackId;

	/** 이 이행을 만든 측정계획. 재개방 시 이 값으로 이력을 되돌린다. */
	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	/**
	 * 원장(시설별 측정물질) 연결키. 측정항목 식별자 도입 이전 스냅샷에는 없으므로 nullable이다.
	 * 회차당 항목 유일성은 {@code pollutant_id}가 보장하므로 제약에는 쓰지 않는다.
	 */
	@Column(name = "stack_pollutant_id")
	private Long stackPollutantId;

	@Column(name = "pollutant_id", nullable = false)
	private Long pollutantId;

	// --- 측정 시점 표시값 스냅샷 ---

	/** 측정물질 가이드 키. 카탈로그 도입 이전 스냅샷은 null이므로 소비처는 이름으로 폴백한다. */
	@Column(name = "pollutant_code", length = 30)
	private String pollutantCode;

	@Column(name = "pollutant_name_kr")
	private String pollutantNameKr;

	// --- 주기 구간 ---

	/** 측정 시점의 주기. 원장에서 주기를 지정하지 않은 항목은 null이다. */
	@Enumerated(EnumType.STRING)
	@Column(name = "cycle")
	private MeasurementCycle cycle;

	/** 구간 연도. 주기가 없는 항목도 연도별 조회에 걸려야 하므로 항상 측정일의 연도가 들어간다. */
	@Column(name = "period_year", nullable = false)
	private Integer periodYear;

	/** 1부터 시작하는 구간 번호. 주기가 없으면 구간을 특정할 수 없으므로 null이다. */
	@Column(name = "period_index")
	private Integer periodIndex;

	@Column(name = "sampled_at", nullable = false)
	private LocalDate sampledAt;

	@Column(name = "completed_at", nullable = false)
	private LocalDateTime completedAt;

	// --- 확정 결과값. 분석 결과 입력 전에는 비어 있다. ---

	@Column(name = "concentration", precision = 19, scale = 6)
	private BigDecimal concentration;

	@Column(name = "unit", length = 30)
	private String unit;

	/** 산소보정 후 농도. 보정 미적용 항목은 null이다. */
	@Column(name = "corrected_concentration", precision = 19, scale = 6)
	private BigDecimal correctedConcentration;

	@Column(name = "emission", precision = 19, scale = 6)
	private BigDecimal emission;

	/** 측정 시점 허용기준. 원장이 개정되어도 과거 판정이 뒤집히지 않도록 복사해 둔다. */
	@Column(name = "allowance", precision = 19, scale = 6)
	private BigDecimal allowance;

	/** 허용기준 초과 여부. 기준이나 비교값이 없어 판정할 수 없으면 null이다. */
	@Column(name = "exceeded")
	private Boolean exceeded;
}
