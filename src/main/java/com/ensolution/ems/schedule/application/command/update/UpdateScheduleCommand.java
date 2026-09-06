package com.ensolution.ems.schedule.application.command.update;

import java.time.LocalDate;

/**
 * 측정계획을 정의하는 값의 수정 커맨드 — 측정일자·측정용도·관리번호.
 *
 * <p>전달값을 그대로 채택한다(빈 값 = 지움). 이 셋은 한 화면이 단독으로 소유하기 때문이다.
 *
 * <p>여기서 다루지 않는 것:
 * <ul>
 *   <li>측정분야 — 생성 시점에만 정한다. 바뀌면 측정항목과 성적서 서식이 통째로 달라진다</li>
 *   <li>대상(stackId·teamId) — 스냅샷 정합성을 위해 생성 이후 변경하지 않는다</li>
 *   <li>시료접수·분석완료·성적서발행 일자 — {@link UpdateBasicInfoCommand} 경로가 부분 갱신으로 맡는다</li>
 *   <li>의뢰기관·사업장·측정시설 스냅샷 — {@code ChangeClientSnapshotCommand} 경로를 쓴다</li>
 * </ul>
 */
public record UpdateScheduleCommand(
	LocalDate sampledAt,
	String schedulePurpose,
	String referenceNumber
) {}
