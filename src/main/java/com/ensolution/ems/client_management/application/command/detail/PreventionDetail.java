package com.ensolution.ems.client_management.application.command.detail;

import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.domain.TargetSubstance;

import java.util.List;

/**
 * 방지시설 상세 조회 결과. 방지시설 본체와 그에 속한 측정대상물질을 조립한 VO.
 * 도메인 모델(Prevention)이 자식을 직접 참조하지 않으므로 조회 시점에만 조립한다.
 */
public record PreventionDetail(
	Prevention prevention,
	List<TargetSubstance> targets
) {}
