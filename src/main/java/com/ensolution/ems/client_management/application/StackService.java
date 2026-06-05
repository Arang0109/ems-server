package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.CreateStackCommand;
import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.application.command.UpdateStackCommand;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.domain.port.StackRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StackService {
	
	private final StackRepository stackRepository;
	
	public Stack createStack(CreateStackCommand command) {
		Stack newStack = Stack.register(
			command.workplaceId(),
			command.field(),
			command.name(),
			command.semsNumber(),
			command.grade(),
			command.businessCategory(),
			command.mainProduct()
		);
		return stackRepository.save(newStack);
	}
	
	public List<StackListItem> getStackList(Long workplaceId) {
		return stackRepository.findByWorkplaceId(workplaceId);
	}

	public Stack getStack(Long stackId) {
		return stackRepository.findById(stackId);
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
}