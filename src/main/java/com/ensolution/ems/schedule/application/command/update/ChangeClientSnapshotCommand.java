package com.ensolution.ems.schedule.application.command.update;

import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;

/**
 * 측정계획 문서의 의뢰기관(→사업장→측정시설) 스냅샷 수정 커맨드.
 * null(문자열은 공백 포함)인 필드는 기존 값을 유지하며, 원장 연결키인 clientId·workplaceId·stackId는 변경 대상이 아니다.
 * {@code workplace}를 전달하면 하위 사업장·측정시설도 같은 규칙으로 부분 병합된다.
 * 배출시설관리자·시료채취입회자 등 담당자는 기본 정보 수정 경로({@code UpdateBasicInfoCommand})에서 다룬다.
 */
public record ChangeClientSnapshotCommand(
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String email,
	String tel,
	WorkplaceSnapshot workplace
) {}
