package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.history.MeasurementRecord;

import java.util.List;

/**
 * 측정항목별 회차 이력(MySQL) 아웃바운드 포트.
 * 기록은 측정계획 완료 시 한꺼번에 추가되고 재개방·취소 시 계획 단위로 지워지므로,
 * 단건 저장·수정 메서드를 두지 않는다.
 */
public interface MeasurementRecordRepository {

	List<MeasurementRecord> saveAll(List<MeasurementRecord> records);

	/** 측정일 내림차순(같은 날은 등록 순)으로 측정시설의 전체 이력을 반환한다. */
	List<MeasurementRecord> findByStackId(Long stackId, Long tenantId);

	/** 측정시설의 특정 연도 이력. 이행 현황판의 입력이다. */
	List<MeasurementRecord> findByStackIdAndYear(Long stackId, Long tenantId, int year);

	/** 테넌트 전체의 특정 연도 이력. 미이행 항목 집계의 입력이다. */
	List<MeasurementRecord> findByTenantIdAndYear(Long tenantId, int year);

	/** 측정계획의 이행 기록을 모두 지운다(재개방·취소 시 이행 해제). 없어도 예외를 던지지 않는다. */
	void deleteByScheduleId(Long scheduleId, Long tenantId);
}
