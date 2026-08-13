package com.ensolution.ems.contract.application.port.in;

import java.time.LocalDate;

/**
 * 만료가 임박한 계약을 타 모듈(대시보드 등)에 공개하는 요약 VO.
 * 잔여일수 같은 표시용 파생값은 담지 않는다. 소비 모듈이 자신의 기준일로 계산한다.
 */
public record ExpiringContractSummary(
	Long contractId,
	String contractName,
	String workplaceName,
	LocalDate completionDate
) {
}
