package com.ensolution.ems.schedule.infrastructure.document;

import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 측정계획 세부 스냅샷 문서. 도메인 스냅샷 record를 그대로 보관하며,
 * Spring Data MongoDB가 유형별 사양(EquipmentSpec) 등 다형성 필드를 {@code _class} 판별자로 복원한다.
 *
 * <p>측정계획의 상태와 성적서 진행 날짜는 <b>메타(MySQL {@code schedules})에만</b> 있다.
 * 두 저장소에 2PC를 걸 수 없어 사본을 두면 어느 쪽이 진실인지 알 수 없기 때문이다.
 *
 * <p>실험분석정보는 {@code items[].analysis}로 이 문서 안에 함께 들어 있다. 실험실 입력과 현장
 * 입력은 상태 머신이 갈라 주어 시간축에서 겹치지 않으며, 같은 단계에 열리는 실험·분석 탭과
 * 성적서 탭은 쓰는 필드가 겹치지 않는다({@code AnalysisResult}).
 */
@Document("schedule_documents")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDocument {
	// 낙관적 락. 읽어온 version으로 저장해야 하므로 도메인 스냅샷이 값을 왕복시킨다.
	// null이면 Spring Data가 신규 문서로 판정해 insert하고 0을 부여한다.
	@Version
	private Long version;

	@Id
	private String id;              // = scheduleId 문자열

	@Indexed
	private Long tenantId;

	private Long scheduleId;

	private ClientSnapshot client;
	private TenantSnapshot tenant;
	private TeamSnapshot team;
	private SamplingSnapshot samplingData;
	private List<SamplingItemSnapshot> items;

	// 도메인이 들고 다니지 않는다(측정 사실이 아니라 저장 메타다). @CreatedDate는 신규 문서에만
	// 값을 채우므로, 갱신 시에는 어댑터가 기존 값을 읽어 되돌린다.
	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime modifiedAt;
}
