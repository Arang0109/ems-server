package com.ensolution.ems.contract.application.port.in;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 계약 통계를 타 모듈(대시보드 등)에 제공하는 인바운드 포트.
 * "계약 만료는 완료일(completionDate) 기준"이라는 도메인 규칙은 소유 모듈인 contract가 정의하고,
 * 몇 개월 이내를 임박으로 볼지는 소비 모듈의 표시 정책이므로 구간을 파라미터로 받는다.
 * 모든 조회는 소유권 격리를 위해 tenantId 범위 안에서만 집계한다.
 */
public interface ContractStatisticsUseCase {

	/** 테넌트의 전체 계약 수. */
	long countContracts(Long tenantId);

	/** 계약일(contractDate) 기준 해당 월에 체결된 계약 수. */
	long countContractsInMonth(Long tenantId, YearMonth yearMonth);

	/** 완료일(completionDate)이 [from, to] 구간인 계약을 임박한 순으로 반환한다. 완료일이 없는 계약은 제외한다. */
	List<ExpiringContractSummary> findExpiringBetween(Long tenantId, LocalDate from, LocalDate to);
}
