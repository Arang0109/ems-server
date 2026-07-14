package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.detail.PreventionDetail;
import com.ensolution.ems.tenant.application.command.detail.StackDetail;
import com.ensolution.ems.tenant.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.tenant.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.tenant.application.port.out.ClientRepository;
import com.ensolution.ems.tenant.application.port.out.PollutantRepository;
import com.ensolution.ems.tenant.application.port.out.StackPollutantRepository;
import com.ensolution.ems.tenant.application.port.out.WorkplaceRepository;
import com.ensolution.ems.tenant.domain.Client;
import com.ensolution.ems.tenant.domain.Facility;
import com.ensolution.ems.tenant.domain.Pollutant;
import com.ensolution.ems.tenant.domain.Stack;
import com.ensolution.ems.tenant.domain.Workplace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 측정 대상(측정시설)과 그 상·하위 트리를 조회 포트로 읽어 측정 시점 스냅샷 요약({@link StackMeasurementSummary})으로
 * 조립한다. 하위 트리(방지시설·배출시설·측정대상물질)는 {@link StackDetailAssembler}를 재사용하고,
 * 상위(사업장·의뢰기관)와 시설별 측정항목은 각 포트로 보강한다.
 */
@Component
@RequiredArgsConstructor
public class StackSnapshotAssembler {

	private final StackDetailAssembler stackDetailAssembler;
	private final WorkplaceRepository workplaceRepository;
	private final ClientRepository clientRepository;
	private final StackPollutantRepository stackPollutantRepository;
	private final PollutantRepository pollutantRepository;

	public StackMeasurementSummary assemble(Long stackId, Long tenantId) {
		StackDetail detail = stackDetailAssembler.assemble(stackId, tenantId);
		Stack stack = detail.stack();
		Workplace workplace = workplaceRepository.findById(stack.getWorkplaceId(), tenantId);
		Client client = clientRepository.findById(workplace.getClientId(), tenantId);

		return new StackMeasurementSummary(
			toClientInfo(client),
			toWorkplaceInfo(workplace),
			toStackInfo(stack),
			detail.facilities().stream().map(this::toFacilityInfo).toList(),
			detail.preventions().stream().map(this::toPreventionInfo).toList(),
			assembleMeasurementItems(stackId, tenantId)
		);
	}

	private List<StackMeasurementSummary.MeasurementItemInfo> assembleMeasurementItems(Long stackId, Long tenantId) {
		List<StackPollutantListItem> stackPollutants = stackPollutantRepository.findByStackId(stackId, tenantId);
		if (stackPollutants.isEmpty()) return List.of();

		Map<Long, Pollutant> pollutantById = pollutantRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(Pollutant::getId, Function.identity(), (a, b) -> a));

		return stackPollutants.stream()
			.map(sp -> {
				Pollutant pollutant = pollutantById.get(sp.pollutantId());
				return new StackMeasurementSummary.MeasurementItemInfo(
					sp.id(),
					sp.pollutantId(),
					sp.nameKr(),
					sp.nameEn(),
					pollutant == null ? null : pollutant.getField(),
					pollutant == null ? null : pollutant.getMethod(),
					pollutant == null ? null : pollutant.getPhase(),
					pollutant == null ? null : pollutant.getEquipment(),
					pollutant == null ? null : pollutant.getTestMethod(),
					sp.cycle(),
					sp.allowance()
				);
			})
			.toList();
	}

	private StackMeasurementSummary.ClientInfo toClientInfo(Client client) {
		return new StackMeasurementSummary.ClientInfo(
			client.getId(), client.getName(), client.getBizNumber(), client.getRepresentative(),
			client.getRoadAddress(), client.getDetailAddress(), client.getZipcode(),
			client.getManager(), client.getEmail(), client.getTel()
		);
	}

	private StackMeasurementSummary.WorkplaceInfo toWorkplaceInfo(Workplace workplace) {
		return new StackMeasurementSummary.WorkplaceInfo(
			workplace.getId(), workplace.getName(), workplace.getBizNumber(),
			workplace.getRoadAddress(), workplace.getDetailAddress(), workplace.getZipcode(), workplace.getGrade()
		);
	}

	private StackMeasurementSummary.StackInfo toStackInfo(Stack stack) {
		return new StackMeasurementSummary.StackInfo(
			stack.getId(), stack.getField(), stack.getName(), stack.getSemsNumber(), stack.getGrade(),
			stack.getBusinessCategory(), stack.getMainProduct(), stack.getStandardOxygen(), stack.getHeight(),
			stack.getHorizontalLength(), stack.getVerticalLength(), stack.getShape(), stack.getOrientation()
		);
	}

	private StackMeasurementSummary.FacilityInfo toFacilityInfo(Facility facility) {
		return new StackMeasurementSummary.FacilityInfo(
			facility.getId(), facility.getName(), facility.getFuelUsage(), facility.getFuelInput(), facility.getFuelType()
		);
	}

	private StackMeasurementSummary.PreventionInfo toPreventionInfo(PreventionDetail preventionDetail) {
		return new StackMeasurementSummary.PreventionInfo(
			preventionDetail.prevention().getId(),
			preventionDetail.prevention().getName(),
			preventionDetail.targets().stream()
				.map(target -> new StackMeasurementSummary.TargetSubstanceInfo(
					target.getId(), target.getName(), target.getRemovalEfficiency()
				))
				.toList()
		);
	}
}
