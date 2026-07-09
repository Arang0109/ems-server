package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.port.out.TargetSubstanceRepository;
import com.ensolution.ems.client_management.domain.TargetSubstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TargetSubstanceQueryService {

	private final TargetSubstanceRepository targetSubstanceRepository;

	public TargetSubstance getTargetSubstance(Long targetSubstanceId) {
		return targetSubstanceRepository.findById(targetSubstanceId);
	}

	public List<TargetSubstance> getTargetSubstanceList(Long preventionId) {
		return targetSubstanceRepository.findByPreventionId(preventionId);
	}
}
