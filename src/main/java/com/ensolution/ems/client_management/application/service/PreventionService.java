package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreatePreventionCommand;
import com.ensolution.ems.client_management.application.command.update.ReorderPreventionsCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePreventionCommand;
import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.application.validator.PreventionValidator;
import com.ensolution.ems.client_management.domain.Prevention;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class PreventionService {

	/** 표시 순서 사이의 간격. 신규 등록을 재정렬 없이 목록 맨 뒤에 붙이기 위해 여유를 둔다. */
	private static final int SORT_ORDER_STEP = 10;

	private final PreventionRepository preventionRepository;
	private final StackRepository stackRepository;
	private final PreventionValidator preventionValidator;

	public Prevention createPrevention(CreatePreventionCommand command) {
		stackRepository.findById(command.stackId(), command.tenantId());
		// 순서를 비워 두면 MySQL 이 ASC 에서 NULL 을 앞에 놓아 새 시설이 목록 맨 앞으로 튄다.
		int sortOrder = preventionRepository.findMaxSortOrder(command.stackId(), command.tenantId()) + SORT_ORDER_STEP;
		return preventionRepository.save(
			Prevention.register(
				command.tenantId(), command.stackId(), command.name(), command.capacity(), command.unit(), command.targetName(), command.removalEfficiency(), sortOrder)
		);
	}

	/**
	 * 방지시설 표시 순서 변경.
	 *
	 * <p>요청 배열이 그 측정지점의 최종 순서 전체다. 검증을 모두 통과한 뒤에 저장해 부분 반영을 막는다.
	 */
	public List<Prevention> reorderPreventions(ReorderPreventionsCommand command) {
		stackRepository.findById(command.stackId(), command.tenantId());

		List<Prevention> current = preventionRepository.findByStackId(command.stackId(), command.tenantId());
		preventionValidator.requireExactOrder(current, command.orderedIds());

		Map<Long, Prevention> byId = current.stream()
			.collect(Collectors.toMap(Prevention::getId, Function.identity()));

		return preventionRepository.saveAll(
			IntStream.range(0, command.orderedIds().size())
				.mapToObj(index -> byId.get(command.orderedIds().get(index)).reorder((index + 1) * SORT_ORDER_STEP))
				.toList()
		);
	}

	public Prevention updatePrevention(Long preventionId, Long tenantId, UpdatePreventionCommand command) {
		Prevention prevention = preventionRepository.findById(preventionId, tenantId);
		return preventionRepository.save(
			prevention.update(command.name(), command.capacity(), command.unit(), command.targetName(), command.removalEfficiency()));
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
