package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 엑셀(jxls) 템플릿이 바인딩하는 측정계획 최상위 뷰. 내부 도메인/스냅샷 구조와 분리된 <b>안정적·평평한 계약</b>이며,
 * 내부 리팩터링이 고객 템플릿을 깨뜨리지 않도록 이 계약을 통해서만 노출한다.
 * 필드명을 바꾸면 배포된 템플릿이 깨지므로 신중히 변경한다(호환 불가 변경 시 버전 분리 고려).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class ScheduleExportView {

	// 기본 정보
	private final String referenceNumber;   // 내부 식별 코드
	private final String measurementField;  // 측정분야 (대기/수질 등)
	private final String schedulePurpose;   // 측정용도
	
	private final LocalDate sampledAt;      // 채취일자
	private final LocalDate receivedAt;     // 시료접수일자
	private final LocalDate analyzedAt;     // 분석완료일자
	private final LocalDate issuedAt;       // 성적서발행일자
	
	private final LocalTime samplingStartedAt;
	private final LocalTime samplingEndedAt;
	
	private final String mentor;
	private final String mentee;
	private final String facilityManager;   // 배출시설관리자
	private final String samplingWitness;   // 시료채취입회자(환경기술인)
	private final String analyst;           // 시료분석검사자
	private final String technicalManager;  // 기술책임자
	
	// 고객사
	private final String tenantName;
	private final String tenantBizNumber;
	private final String tenantRepresentative;
	private final String tenantAddress;
	
	// 의뢰기관
	private final String clientName;
	private final String clientBizNumber;
	private final String clientRepresentative;
	private final String clientAddress;

	// 사업장
	private final String workplaceName;
	private final String workplaceBizNumber;
	private final String workplaceAddress;
	private final String workplaceGrade;

	// 측정시설(굴뚝)
	private final String stackName;
	private final String semsNumber;
	private final String businessCategory;
	private final String mainProduct;
	private final String stackGrade;
	private final String stackShape;        // 원형/사각형
	private final String stackOrientation;
	private final Double horizontalLength;
	private final Double verticalLength;
	private final Double height;
	private final Integer standardOxygen;

	// 배출·방지시설 (jx:each 대상)
	private final List<FacilityExportView> facilities;
	private final List<PreventionExportView> preventions;
	private final List<TargetSubstanceExportView> targetSubstances;  // 방지시설 구분 없이 평탄화한 전체 목록

	// 측정 장비 (팀 슬롯별 단건 참조)
	private final EquipmentExportView particleSampler;  // 입자상 채취기
	private final EquipmentExportView gasSampler;       // 가스 채취기
	private final EquipmentExportView pitotTube;        // 피토관
	private final EquipmentExportView nozzle;           // 노즐

	// 측정 장비 목록 (jx:each 대상)
	private final List<EquipmentExportView> equipments;

	// 측정 시트 목록 (jx:each 대상)
	private final List<SheetExportView> sheets;
}
