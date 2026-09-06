package com.ensolution.ems.dashboard.application.mapper;

import com.ensolution.ems.dashboard.application.command.MeasurementCountItem;
import com.ensolution.ems.schedule.application.port.in.MonthlyMeasurementSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 인터모듈 매퍼: 공급 모듈(schedule)의 {@code port/in} 요약 VO를 대시보드 표시용 VO로 변환한다.
 * 기간 라벨("N월")은 대시보드가 붙이는 표시값이며, 집계 자체는 소유 모듈인 schedule이 정의한다.
 */
@Mapper(componentModel = "spring")
public interface SchedulePortMapper {

	@Mapping(target = "label", expression = "java(summary.month() + \"월\")")
	MeasurementCountItem toMeasurementCountItem(MonthlyMeasurementSummary summary);

	List<MeasurementCountItem> toMeasurementCountItems(List<MonthlyMeasurementSummary> summaries);
}
