package com.ensolution.ems.schedule.infrastructure.document;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.snapshot.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 측정계획 세부 스냅샷 문서. 도메인 스냅샷 record를 그대로 보관하며,
 * Spring Data MongoDB가 유형별 사양(EquipmentSpec) 등 다형성 필드를 {@code _class} 판별자로 복원한다.
 */
@Document("schedule_documents")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDocument {

	@Id
	private String id;              // = scheduleId 문자열

	@Indexed
	private Long tenantId;

	private Long scheduleId;
	
	private ScheduleStatus status;
	private BasicInfo basicInfo;
	private TeamSnapshot team;
	private TenantSnapshot tenant;
	private ClientSnapshot client;
	private List<EquipmentSnapshot> equipments;
	private List<SamplingItemSnapshot> items;
	private List<MeasurementSheet> sheets;

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime modifiedAt;
}
