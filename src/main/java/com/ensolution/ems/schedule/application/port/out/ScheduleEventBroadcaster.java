package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.application.command.event.SheetsSavedEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 측정계획 편집 이벤트를 같은 계획을 열어둔 클라이언트에 실시간으로 전달하는 아웃바운드 포트.
 * <p>
 * 시그니처에 {@link SseEmitter}(spring-web)가 드러난다. 자체 스트림 추상화를 세우는 편이
 * 형식상 깔끔하지만 구현이 하나뿐인 지금은 얻는 것 없이 간접 계층만 늘어난다.
 * 계층 규칙이 금지하는 것은 {@code domain → spring}과 {@code application → infrastructure}이며
 * 둘 다 해당하지 않는다.
 */
public interface ScheduleEventBroadcaster {

	/** 구독자를 등록한다. 연결 종료·타임아웃·전송 실패 시 구현체가 스스로 정리한다. */
	void subscribe(Long scheduleId, Long tenantId, SseEmitter emitter);

	/** 같은 측정계획을 구독 중인 클라이언트에 시트 저장을 알린다. */
	void publishSheetsSaved(SheetsSavedEvent event);
}
