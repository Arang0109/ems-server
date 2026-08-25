package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.springframework.stereotype.Component;

/**
 * 측정계획 메타(MySQL)와 세부 스냅샷(MongoDB)을 목록 아이템({@link ScheduleListItem})으로 합친다.
 * 표시용 이름은 스냅샷 트리(의뢰기관 → 사업장 → 측정시설)에서 뽑으므로 각 단계의 null을 방어적으로 다룬다.
 * <p>
 * 두 저장소 조회와 scheduleId 색인은 호출부({@code ScheduleService})가 맡고 여기서는 <b>순수 변환만</b>
 * 한다 — 포트를 주입받지 않는다.
 */
@Component
public class ScheduleListItemMapper {

	/**
	 * 메타에 세부 스냅샷을 합쳐 목록 아이템을 만든다.
	 *
	 * @param snapshot 해당 계획의 세부 문서. null이면 표시용 이름을 채우지 않는다
	 */
	public ScheduleListItem toListItem(Schedule meta, ScheduleSnapshot snapshot) {
		return new ScheduleListItem(
			meta.getId(),
			meta.getStackId(),
			meta.getTeamId(),
			meta.getMeasurementField(),
			meta.getSampledAt(),
			meta.getSchedulePurpose(),
			meta.getStatus(),
			meta.getReferenceNumber(),
			clientName(snapshot),
			stackName(snapshot),
			teamName(snapshot),
			meta.getCreatedAt()
		);
	}

	private String clientName(ScheduleSnapshot snapshot) {
		if (snapshot == null || snapshot.client() == null) return null;
		return snapshot.client().name();
	}

	private String stackName(ScheduleSnapshot snapshot) {
		if (snapshot == null) return null;
		ClientSnapshot client = snapshot.client();
		if (client == null || client.workplace() == null || client.workplace().stack() == null) return null;
		return client.workplace().stack().name();
	}

	private String teamName(ScheduleSnapshot snapshot) {
		if (snapshot == null || snapshot.team() == null) return null;
		return snapshot.team().teamName();
	}
}
