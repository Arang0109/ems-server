package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.create.CreateCustomFieldDefinitionCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateCustomFieldDefinitionCommand;
import com.ensolution.ems.schedule.application.port.out.CustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.application.validator.CustomFieldDefinitionValidator;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 커스텀 필드 정의 유스케이스. 다른 모듈이 소비하지 않으므로 인바운드 포트를 두지 않는다.
 *
 * <p>정의는 이름(키·라벨)만 가진다. 값은 회차마다 스냅샷 문서가 갖고({@code ScheduleSnapshotService#saveCustomFields}),
 * 템플릿은 {@code ${custom.<key>}}로 읽는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomFieldDefinitionService {

	private static final int SORT_ORDER_STEP = 10;

	private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
	private final CustomFieldDefinitionValidator customFieldDefinitionValidator;

	/** 등록. {@code sortOrder}를 주지 않으면 목록 맨 뒤({@code max+10})에 붙인다. */
	public CustomFieldDefinition createDefinition(CreateCustomFieldDefinitionCommand command) {
		customFieldDefinitionValidator.requireUniqueKey(command.key(), command.tenantId());

		Integer sortOrder = command.sortOrder() != null
			? command.sortOrder()
			: customFieldDefinitionRepository.findMaxSortOrder(command.tenantId()) + SORT_ORDER_STEP;

		return customFieldDefinitionRepository.save(
			CustomFieldDefinition.register(command.tenantId(), command.key(), command.label(), sortOrder));
	}

	public CustomFieldDefinition updateDefinition(Long id, Long tenantId, UpdateCustomFieldDefinitionCommand command) {
		CustomFieldDefinition definition = customFieldDefinitionRepository.findById(id, tenantId);
		return customFieldDefinitionRepository.save(definition.update(command.label(), command.sortOrder()));
	}

	/**
	 * 삭제. <b>스냅샷 문서에 남은 값은 건드리지 않는다.</b> 스냅샷은 측정 시점의 불변 사본이고, 정의 삭제가
	 * 문서 락을 공유하는 경로에 끼어들 이유가 없다. 남은 값은 {@code ${custom.<key>}}로 계속 출력되며,
	 * 그 회차의 커스텀 필드를 다음에 저장할 때 전체 채택으로 맵이 교체되면서 사라진다.
	 */
	public void deleteDefinition(Long id, Long tenantId) {
		customFieldDefinitionRepository.deleteById(id, tenantId);
	}

	@Transactional(readOnly = true)
	public List<CustomFieldDefinition> getDefinitionList(Long tenantId) {
		return customFieldDefinitionRepository.findAll(tenantId);
	}
}
