package com.ensolution.ems.schedule.application.command.update;

/**
 * 측정계획 문서의 고객사(측정대행업체) 스냅샷 수정 커맨드.
 *
 * <p>전달되지 않은(공백 포함) 필드는 기존 값을 유지하는 <b>부분 갱신</b>이다 — 성적서 서명란 담당자를
 * 현장 채취 탭과 실험·분석 탭이 공유하므로, 자기 것이 아닌 칸에 null을 실은 호출자가 상대의 입력을
 * 지우지 않아야 한다. 원장(테넌트)은 이 경로로 바뀌지 않는다.
 */
public record ChangeTenantSnapshotCommand(
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String analyst,
	String technicalManager
) {}
