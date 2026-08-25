package com.ensolution.ems.schedule.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 실험분석정보 문서. 조회는 항상 측정계획 단위이므로 (tenantId, scheduleId) 복합 인덱스를 둔다.
 * 측정 시트가 들어 있는 {@link ScheduleDocument} 와 컬렉션을 분리해, 실험실 입력이
 * 시트 저장의 문서 단위 낙관적 락과 부딪히지 않게 한다.
 */
@Document("analysis_records")
@CompoundIndex(name = "idx_analysis_tenant_schedule", def = "{'tenantId': 1, 'scheduleId': 1}")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRecordDocument {

	@Id
	private String id;

	private Long tenantId;
	private Long scheduleId;

	private Long stackPollutantId;
	private Long pollutantId;
	private String pollutantName;

	private BigDecimal allowance;           // 허용기준치 (측정 시점 원장 사본)
	private boolean oxygenApplicable;       // 기준산소농도 보정 적용 여부 (측정 시점 원장 사본)

	private BigDecimal analysisValue;       // 측정분석값
	private String unit;                    // 측정단위
	private String analysisMethod;          // 측정분석방법
	private String analysisEquipment;       // 분석장비

	// 성적서 입력값. 현장 채취 시트에서 파생하지 않고 성적서 탭에서 항목별로 직접 작성한다.
	private LocalTime samplingStartedAt;    // 채취 시작시각
	private LocalTime samplingEndedAt;      // 채취 종료시각

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime modifiedAt;
}
