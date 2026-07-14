package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreateStackCommand;
import com.ensolution.ems.tenant.application.command.detail.StackDetail;
import com.ensolution.ems.tenant.application.command.list_item.StackListItem;
import com.ensolution.ems.tenant.application.command.update.UpdateStackCommand;
import com.ensolution.ems.tenant.application.port.out.StackRepository;
import com.ensolution.ems.tenant.domain.Stack;
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
			command.tenantId(),
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

	public Stack updateStack(Long stackId, Long tenantId, UpdateStackCommand command) {
		Stack stack = stackRepository.findById(stackId, tenantId);

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
}
