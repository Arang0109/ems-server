package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.port.out.ScheduleEventBroadcaster;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 측정계획 편집 이벤트 구독 유스케이스.
 * <p>
 * 구독을 열기 전에 측정계획의 존재와 tenant 소속을 확인한다. 이 검증이 없으면 id만 바꿔가며
 * 다른 고객사의 편집 알림을 받아볼 수 있다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleStreamService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleEventBroadcaster eventBroadcaster;

	@Transactional(readOnly = true)
	public void subscribe(Long scheduleId, Long tenantId, SseEmitter emitter) {
		// 반환값을 쓰지 않는다 — 어댑터가 없거나 남의 것이면 NOT_FOUND를 던지므로 검증 자체가 목적이다.
		scheduleRepository.findById(scheduleId, tenantId);
		eventBroadcaster.subscribe(scheduleId, tenantId, emitter);
	}
}
