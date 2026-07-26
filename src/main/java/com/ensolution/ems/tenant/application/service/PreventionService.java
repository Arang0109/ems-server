package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreatePreventionCommand;
import com.ensolution.ems.tenant.application.command.update.UpdatePreventionCommand;
import com.ensolution.ems.tenant.application.port.out.PreventionRepository;
import com.ensolution.ems.tenant.application.port.out.StackRepository;
import com.ensolution.ems.tenant.domain.Prevention;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PreventionService {

	private final PreventionRepository preventionRepository;
	private final StackRepository stackRepository;

	public Prevention createPrevention(CreatePreventionCommand command) {
		stackRepository.findById(command.stackId(), command.tenantId());
		return preventionRepository.save(
			Prevention.register(command.tenantId(), command.stackId(), command.name(), command.capacity())
		);
	}

	public Prevention updatePrevention(Long preventionId, Long tenantId, UpdatePreventionCommand command) {
		Prevention prevention = preventionRepository.findById(preventionId, tenantId);
		return preventionRepository.save(prevention.update(command.name(), command.capacity()));
	}

	public void deletePrevention(Long preventionId, Long tenantId) {
		preventionRepository.deleteById(preventionId, tenantId);
	}

	@Transactional(readOnly = true)
	public Prevention getPrevention(Long preventionId, Long tenantId) {
		return preventionRepository.findById(preventionId, tenantId);
	}

	@Transactional(readOnly = true)
	public List<Prevention> getPreventionList(Long stackId, Long tenantId) {
		return preventionRepository.findByStackId(stackId, tenantId);
	}
}
