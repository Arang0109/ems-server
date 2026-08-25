package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateFacilityCommand;
import com.ensolution.ems.client_management.application.command.update.ReorderFacilitiesCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateFacilityCommand;
import com.ensolution.ems.client_management.application.port.out.FacilityRepository;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.application.validator.FacilityValidator;
import com.ensolution.ems.client_management.domain.Facility;
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
public class FacilityService {

	/** 표시 순서 사이의 간격. 신규 등록을 재정렬 없이 목록 맨 뒤에 붙이기 위해 여유를 둔다. */
	private static final int SORT_ORDER_STEP = 10;

	private final FacilityRepository facilityRepository;
	private final StackRepository stackRepository;
	private final FacilityValidator facilityValidator;

	public Facility createFacility(CreateFacilityCommand command) {
		stackRepository.findById(command.stackId(), command.tenantId());
		// 순서를 비워 두면 MySQL 이 ASC 에서 NULL 을 앞에 놓아 새 시설이 목록 맨 앞으로 튄다.
		int sortOrder = facilityRepository.findMaxSortOrder(command.stackId(), command.tenantId()) + SORT_ORDER_STEP;
		return facilityRepository.save(
			Facility.register(
				command.tenantId(), command.stackId(), command.name(), command.fuelUsage(), command.productOutput(),
				command.incinerationAmount(), command.fuelInput(), command.fuelType(), command.unit(), sortOrder)
		);
	}

	/**
	 * 배출시설 표시 순서 변경.
	 *
	 * <p>요청 배열이 그 측정지점의 최종 순서 전체다. 검증을 모두 통과한 뒤에 저장해 부분 반영을 막는다.
	 */
	public List<Facility> reorderFacilities(ReorderFacilitiesCommand command) {
		stackRepository.findById(command.stackId(), command.tenantId());

		List<Facility> current = facilityRepository.findByStackId(command.stackId(), command.tenantId());
		facilityValidator.requireExactOrder(current, command.orderedIds());

		Map<Long, Facility> byId = current.stream()
			.collect(Collectors.toMap(Facility::getId, Function.identity()));

		return facilityRepository.saveAll(
			IntStream.range(0, command.orderedIds().size())
				.mapToObj(index -> byId.get(command.orderedIds().get(index)).reorder((index + 1) * SORT_ORDER_STEP))
				.toList()
		);
	}

	public Facility updateFacility(Long facilityId, Long tenantId, UpdateFacilityCommand command) {
		Facility facility = facilityRepository.findById(facilityId, tenantId);
		return facilityRepository.save(
			facility.update(
				command.name(), command.fuelUsage(), command.productOutput(),
				command.incinerationAmount(), command.fuelInput(), command.fuelType(), command.unit())
		);
	}

	public void deleteFacility(Long facilityId, Long tenantId) {
		facilityRepository.deleteById(facilityId, tenantId);
	}

	@Transactional(readOnly = true)
	public Facility getFacility(Long facilityId, Long tenantId) {
		return facilityRepository.findById(facilityId, tenantId);
	}

	@Transactional(readOnly = true)
	public List<Facility> getFacilityList(Long stackId, Long tenantId) {
		return facilityRepository.findByStackId(stackId, tenantId);
	}
}
