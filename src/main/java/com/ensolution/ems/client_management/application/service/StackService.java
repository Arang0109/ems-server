package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateStackCommand;
import com.ensolution.ems.client_management.application.command.detail.StackDetail;
import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateStackCommand;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.client_management.application.port.in.StackQueryUseCase;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.application.service.assembler.StackDetailAssembler;
import com.ensolution.ems.client_management.application.service.assembler.StackSnapshotAssembler;
import com.ensolution.ems.client_management.application.validator.StackValidator;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StackService implements StackQueryUseCase {

	private final StackRepository stackRepository;
	private final StackPollutantRepository stackPollutantRepository;
	private final StackValidator stackValidator;
	private final StackDetailAssembler stackDetailAssembler;
	private final StackSnapshotAssembler stackSnapshotAssembler;

	public Stack createStack(CreateStackCommand command) {
		Long workplaceId = command.workplaceId();
		MeasurementField field = command.field();
		String name = command.name();

		stackValidator.requireUniqueNameInWorkplace(name, workplaceId, field);

		Stack newStack = Stack.register(
			command.tenantId(),
			workplaceId,
			field,
			name,
			command.semsNumber(),
			command.grade(),
			command.mainProduct(),
			command.standardOxygen()
		);
		return stackRepository.save(newStack);
	}

	public Stack updateStack(Long stackId, Long tenantId, UpdateStackCommand command) {
		Stack stack = stackRepository.findById(stackId, tenantId);

		Stack updated = stack.update(
			command.field(),
			command.name(),
			command.semsNumber(),
			command.grade(),
			command.mainProduct(),
			command.standardOxygen(),
			command.height(),
			command.horizontalLength(),
			command.verticalLength(),
			command.shape(),
			command.orientation()
		);
		return stackRepository.save(updated);
	}

	public void deleteStack(Long stackId, Long tenantId) { stackRepository.deleteById(stackId, tenantId); }

	@Transactional(readOnly = true)
	public List<StackListItem> getStackList(Long workplaceId, Long tenantId) {
		if (workplaceId == null) return stackRepository.findAll(tenantId);
		return stackRepository.findByWorkplaceId(workplaceId, tenantId);
	}

	@Transactional(readOnly = true)
	public StackDetail getStackDetail(Long stackId, Long tenantId) {
		return stackDetailAssembler.assemble(stackId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public StackMeasurementSummary getMeasurementTargetSummary(Long stackId, Long tenantId) {
		return stackSnapshotAssembler.assemble(stackId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countStacks(Long tenantId) {
		return stackRepository.findAll(tenantId).size();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StackMeasurementItemSummary> findMeasurementItems(Long tenantId, Long workplaceId, Long stackId) {
		return stackPollutantRepository.findMeasurementItems(tenantId, workplaceId, stackId);
	}
}
