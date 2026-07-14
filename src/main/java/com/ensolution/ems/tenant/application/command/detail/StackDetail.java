package com.ensolution.ems.tenant.application.command.detail;

import com.ensolution.ems.tenant.domain.Facility;
import com.ensolution.ems.tenant.domain.Stack;

import java.util.List;

/**
 * 측정시설 상세 조회 결과. 측정시설 본체와 그에 속한 방지시설·배출시설을 조립한 VO.
 * 도메인 모델(Stack)이 자식 aggregate를 직접 참조하지 않으므로 조회 시점에만 조립한다.
 */
public record StackDetail(
	Stack stack,
	List<PreventionDetail> preventions,
	List<Facility> facilities
) {}
