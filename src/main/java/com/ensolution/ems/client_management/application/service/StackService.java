package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateStackCommand;
import com.ensolution.ems.client_management.application.command.detail.StackDetail;
import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateStackCommand;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StackService {

	private final StackRepository stackRepository;
	private final StackDetailAssembler stackDetailAssembler;

	public Stack createStack(CreateStackCommand command) {
		Long workplaceId = command.workplaceId();
		MeasurementField field = command.field();
		String name = command.name();

		if (stackRepository.existsByNameAndWorkplaceIdAndField(name, workplaceId, field)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}

		Stack newStack = Stack.register(
			workplaceId,
			field,
			name,
			command.semsNumber(),
			command.grade(),
			command.businessCategory(),
			command.mainProduct()
		);
		return stackRepository.save(newStack);
	}

	public Stack updateStack(Long stackId, UpdateStackCommand command) {
		Stack stack = stackRepository.findById(stackId);

		Stack updated = stack.update(
			command.field(),
			command.name(),
			command.semsNumber(),
			command.grade(),
			command.businessCategory(),
			command.mainProduct(),
			command.height(),
			command.horizontalLength(),
			command.verticalLength(),
			command.shape(),
			command.orientation()
		);
		return stackRepository.save(updated);
	}

	public void deleteStack(Long stackId) { stackRepository.deleteById(stackId); }

	@Transactional(readOnly = true)
	public List<StackListItem> getStackList(Long workplaceId) {
		if (workplaceId == null) return stackRepository.findAll();
		return stackRepository.findByWorkplaceId(workplaceId);
	}

	@Transactional(readOnly = true)
	public StackDetail getStackDetail(Long stackId) {
		return stackDetailAssembler.assemble(stackId);
	}
}
