package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 인메모리 {@link StackPollutantRepository}. */
public class FakeStackPollutantRepository implements StackPollutantRepository {

	private final List<StackPollutant> stackPollutants = new ArrayList<>();
	private long nextId = 1000L;

	private int saveCount = 0;

	public int saveCount() {
		return saveCount;
	}

	public List<StackPollutant> all() {
		return List.copyOf(stackPollutants);
	}

	@Override
	public StackPollutant save(StackPollutant stackPollutant) {
		saveCount++;
		StackPollutant saved = stackPollutant.getId() == null
			? stackPollutant.toBuilder().id(nextId++).build()
			: stackPollutant;
		stackPollutants.removeIf(sp -> Objects.equals(sp.getId(), saved.getId()));
		stackPollutants.add(saved);
		return saved;
	}

	@Override
	public StackPollutant findById(Long id, Long tenantId) {
		return stackPollutants.stream()
			.filter(sp -> Objects.equals(sp.getId(), id) && Objects.equals(sp.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<StackPollutantListItem> findByStackId(Long stackId, Long tenantId) {
		return stackPollutants.stream()
			.filter(sp -> Objects.equals(sp.getStackId(), stackId))
			.filter(sp -> Objects.equals(sp.getTenantId(), tenantId))
			.map(sp -> new StackPollutantListItem(
				sp.getId(), sp.getStackId(), sp.getPollutantId(),
				null, null, null,
				sp.getCycle(), sp.getAllowance(), sp.isOxygenApplicable()))
			.toList();
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		stackPollutants.removeIf(sp ->
			Objects.equals(sp.getId(), id) && Objects.equals(sp.getTenantId(), tenantId));
	}

	@Override
	public boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId) {
		return stackPollutants.stream().anyMatch(sp ->
			Objects.equals(sp.getStackId(), stackId) && Objects.equals(sp.getPollutantId(), pollutantId));
	}

	/**
	 * 측정항목 조회는 카탈로그 조인 결과라 인메모리로 재현할 값이 없다.
	 * 현재 이 페이크를 쓰는 테스트는 이 경로를 타지 않으므로 빈 목록을 돌려준다 —
	 * 필요해지면 그때 저장된 값에서 조립한다.
	 */
	@Override
	public List<StackMeasurementItemSummary> findMeasurementItems(Long tenantId, Long workplaceId, Long stackId) {
		return List.of();
	}
}
