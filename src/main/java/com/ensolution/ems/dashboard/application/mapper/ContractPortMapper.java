package com.ensolution.ems.dashboard.application.mapper;

import com.ensolution.ems.contract.application.port.in.ExpiringContractSummary;
import com.ensolution.ems.dashboard.application.command.ExpiringContract;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 인터모듈 매퍼: 공급 모듈(contract)의 {@code port/in} 요약 VO를 대시보드 표시용 VO로 변환한다.
 * 잔여일수는 대시보드가 기준일(baseDate)을 정해 계산하는 파생값이다.
 */
@Mapper(componentModel = "spring", imports = ChronoUnit.class)
public interface ContractPortMapper {

	@Mapping(target = "daysRemaining",
		expression = "java(ChronoUnit.DAYS.between(baseDate, summary.completionDate()))")
	ExpiringContract toExpiringContract(ExpiringContractSummary summary, @Context LocalDate baseDate);

	List<ExpiringContract> toExpiringContracts(List<ExpiringContractSummary> summaries, @Context LocalDate baseDate);
}
