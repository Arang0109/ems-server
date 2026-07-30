package com.ensolution.ems.contract.application.event;

import com.ensolution.ems.client_management.application.event.WorkplaceDeletedEvent;
import com.ensolution.ems.contract.application.port.ContractQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkplaceDeletedEventListener {

	private final ContractQueryUseCase contractQueryUseCase;

	@EventListener
	public void handle(WorkplaceDeletedEvent event) {
		contractQueryUseCase.deleteContracts(event.workplaceId());
	}
}
