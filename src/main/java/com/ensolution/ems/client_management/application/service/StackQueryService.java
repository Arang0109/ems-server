package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.detail.StackDetail;
import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StackQueryService {

	private final StackRepository stackRepository;
	private final StackDetailAssembler stackDetailAssembler;

	public List<StackListItem> getStackList(Long workplaceId) {
		if (workplaceId == null) return stackRepository.findAll();
		return stackRepository.findByWorkplaceId(workplaceId);
	}

	public StackDetail getStackDetail(Long stackId) {
		return stackDetailAssembler.assemble(stackId);
	}
}
