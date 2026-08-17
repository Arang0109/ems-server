package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.application.service.assembler.PollutantCatalogAssembler;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import com.ensolution.ems.client_management.application.validator.StackPollutantValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StackPollutantService {

	private final StackPollutantRepository stackPollutantRepository;
	private final StackPollutantValidator stackPollutantValidator;
	private final PollutantMaterializer pollutantMaterializer;
	private final PollutantCatalogAssembler pollutantCatalogAssembler;

	public StackPollutant createStackPollutant(CreateStackPollutantCommand command) {
		CreateStackPollutantCommand resolved = resolve(command);
		stackPollutantValidator.requireNotRegistered(resolved.stackId(), resolved.pollutantId());

		return stackPollutantRepository.save(
			StackPollutant.register(
				resolved.tenantId(), resolved.stackId(), resolved.pollutantId(),
				resolved.cycle(), resolved.allowance(), resolved.oxygenApplicable()));
	}

	/**
	 * 측정물질 참조를 먼저 모두 해석한 뒤 중복을 검사한다.
	 * 순서를 바꾸면 id로 지정한 항목과 code로 지정한 항목이 같은 물질이어도 중복으로 잡히지 않는다.
	 */
	public List<StackPollutant> createStackPollutants(List<CreateStackPollutantCommand> commands) {
		List<CreateStackPollutantCommand> resolved = commands.stream().map(this::resolve).toList();
		stackPollutantValidator.requireNoDuplicatesInBatch(resolved);

		return resolved.stream().map(command -> {
			stackPollutantValidator.requireNotRegistered(command.stackId(), command.pollutantId());
			return stackPollutantRepository.save(
				StackPollutant.register(
					command.tenantId(), command.stackId(), command.pollutantId(),
					command.cycle(), command.allowance(), command.oxygenApplicable()));
		}).toList();
	}

	public void removeStackPollutant(Long id, Long tenantId) {
		stackPollutantRepository.deleteById(id, tenantId);
	}

	/** 물질명은 고객사가 보유한 측정물질 값으로 채운다(code는 카탈로그 투영값). */
	@Transactional(readOnly = true)
	public List<StackPollutantListItem> getStackPollutantList(Long stackId, Long tenantId) {
		List<StackPollutantListItem> items = stackPollutantRepository.findByStackId(stackId, tenantId);
		if (items.isEmpty()) return items;

		Map<Long, Pollutant> pollutantById = pollutantCatalogAssembler.pollutantById(tenantId);

		return items.stream().map(item -> {
			Pollutant pollutant = pollutantById.get(item.pollutantId());
			if (pollutant == null) return item;
			return new StackPollutantListItem(
				item.id(),
				item.stackId(),
				item.pollutantId(),
				pollutant.getCode(),
				pollutant.getNameKr(),
				pollutant.getNameEn(),
				item.cycle(),
				item.allowance(),
				item.oxygenApplicable()
			);
		}).toList();
	}

	private CreateStackPollutantCommand resolve(CreateStackPollutantCommand command) {
		return command.withPollutantId(pollutantMaterializer.resolvePollutantId(
			command.pollutantId(), command.catalogId(), command.tenantId()));
	}
}
