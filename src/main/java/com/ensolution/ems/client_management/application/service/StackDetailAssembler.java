package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.detail.PreventionDetail;
import com.ensolution.ems.client_management.application.command.detail.StackDetail;
import com.ensolution.ems.client_management.application.port.out.FacilityRepository;
import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.application.port.out.TargetSubstanceRepository;
import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.client_management.domain.Stack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 측정시설(Stack)과 그 하위 aggregate(방지시설·배출시설·측정대상물질)를 조회 포트로 각각 읽어
 * {@link StackDetail}로 조립한다. 트리 조립 책임을 도메인 모델에서 분리한다.
 */
@Component
@RequiredArgsConstructor
public class StackDetailAssembler {

	private final StackRepository stackRepository;
	private final PreventionRepository preventionRepository;
	private final FacilityRepository facilityRepository;
	private final TargetSubstanceRepository targetSubstanceRepository;

	public StackDetail assemble(Long stackId) {
		Stack stack = stackRepository.findById(stackId);
		List<Facility> facilities = facilityRepository.findByStackId(stackId);
		List<PreventionDetail> preventions = preventionRepository.findByStackId(stackId).stream()
			.map(prevention -> new PreventionDetail(
				prevention,
				targetSubstanceRepository.findByPreventionId(prevention.getId())
			))
			.toList();
		return new StackDetail(stack, preventions, facilities);
	}
}
