package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StackPollutantQueryService {

	private final StackPollutantRepository stackPollutantRepository;

	public List<StackPollutantListItem> getStackPollutantList(Long stackId) {
		return stackPollutantRepository.findByStackId(stackId);
	}
}
