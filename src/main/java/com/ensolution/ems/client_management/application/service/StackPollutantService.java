package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateStackPollutantCommand;
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
	private final PollutantCatalogAssembler pollutantCatalogAssembler;

	/** 등록 대상은 이 고객사가 이미 채택한 측정물질이어야 한다. 채택은 {@code PollutantService}가 담당한다. */
	public StackPollutant createStackPollutant(CreateStackPollutantCommand command) {
		stackPollutantValidator.requirePollutantOwned(command.pollutantId(), command.tenantId());
		stackPollutantValidator.requireNotRegistered(command.stackId(), command.pollutantId());

		return stackPollutantRepository.save(
			StackPollutant.register(
				command.tenantId(), command.stackId(), command.pollutantId(),
				command.cycle(), command.allowance(), command.oxygenApplicable()));
	}

	/** 요청 전체를 먼저 검증한 뒤 저장해 부분 저장을 막는다. tenantId는 컨트롤러가 모든 항목에 같은 값으로 채운다. */
	public List<StackPollutant> createStackPollutants(List<CreateStackPollutantCommand> commands) {
		stackPollutantValidator.requirePollutantsOwned(commands, commands.getFirst().tenantId());
		stackPollutantValidator.requireNoDuplicatesInBatch(commands);

		return commands.stream().map(command -> {
			stackPollutantValidator.requireNotRegistered(command.stackId(), command.pollutantId());
			return stackPollutantRepository.save(
				StackPollutant.register(
					command.tenantId(), command.stackId(), command.pollutantId(),
					command.cycle(), command.allowance(), command.oxygenApplicable()));
		}).toList();
	}

	/** 측정 조건(주기·허용기준·산소보정)만 수정한다. 물질을 바꾸는 것은 삭제 후 재등록이다. */
	public StackPollutant updateStackPollutant(Long id, Long tenantId, UpdateStackPollutantCommand command) {
		StackPollutant stackPollutant = stackPollutantRepository.findById(id, tenantId);

		return stackPollutantRepository.save(stackPollutant.update(
			command.cycle(), command.allowance(), command.oxygenApplicable()
		));
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
}
