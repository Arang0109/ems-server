package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.domain.Prevention;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreventionQueryService {

	private final PreventionRepository preventionRepository;

	public Prevention getPrevention(Long preventionId) {
		return preventionRepository.findById(preventionId);
	}

	public List<Prevention> getPreventionList(Long stackId) {
		return preventionRepository.findByStackId(stackId);
	}
}
