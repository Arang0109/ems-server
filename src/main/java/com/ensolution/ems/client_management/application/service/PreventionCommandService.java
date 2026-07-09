package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreatePreventionCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePreventionCommand;
import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.domain.Prevention;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreventionCommandService {

	private final PreventionRepository preventionRepository;
	private final StackRepository stackRepository;

	public Prevention createPrevention(CreatePreventionCommand command) {
		stackRepository.findById(command.stackId());
		return preventionRepository.save(
			Prevention.register(command.stackId(), command.name())
		);
	}

	public Prevention updatePrevention(Long preventionId, UpdatePreventionCommand command) {
		Prevention prevention = preventionRepository.findById(preventionId);
		return preventionRepository.save(prevention.update(command.name()));
	}

	public void deletePrevention(Long preventionId) {
		preventionRepository.deleteById(preventionId);
	}
}
