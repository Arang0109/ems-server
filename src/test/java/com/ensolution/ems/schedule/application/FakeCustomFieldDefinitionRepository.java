package com.ensolution.ems.schedule.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.CustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 인메모리 {@link CustomFieldDefinitionRepository}. tenant 필터와 예외를 실제 어댑터와 같게 재현한다. */
public class FakeCustomFieldDefinitionRepository implements CustomFieldDefinitionRepository {

	private final List<CustomFieldDefinition> definitions = new ArrayList<>();
	private long nextId = 1L;

	/** 테스트 픽스처 등록. */
	public CustomFieldDefinition given(Long tenantId, String key, String label, Integer sortOrder) {
		return save(CustomFieldDefinition.register(tenantId, key, label, sortOrder));
	}

	@Override
	public CustomFieldDefinition save(CustomFieldDefinition definition) {
		CustomFieldDefinition saved = definition.getId() == null
			? definition.toBuilder().id(nextId++).build()
			: definition;
		definitions.removeIf(d -> Objects.equals(d.getId(), saved.getId()));
		definitions.add(saved);
		return saved;
	}

	@Override
	public CustomFieldDefinition findById(Long id, Long tenantId) {
		return definitions.stream()
			.filter(d -> Objects.equals(d.getId(), id) && Objects.equals(d.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_FIELD_NOT_FOUND));
	}

	@Override
	public List<CustomFieldDefinition> findAll(Long tenantId) {
		return definitions.stream()
			.filter(d -> Objects.equals(d.getTenantId(), tenantId))
			.sorted(Comparator.comparing(CustomFieldDefinition::getSortOrder,
					Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(CustomFieldDefinition::getId))
			.toList();
	}

	@Override
	public boolean existsByKeyAndTenantId(String key, Long tenantId) {
		return definitions.stream()
			.anyMatch(d -> Objects.equals(d.getKey(), key) && Objects.equals(d.getTenantId(), tenantId));
	}

	@Override
	public Integer findMaxSortOrder(Long tenantId) {
		return definitions.stream()
			.filter(d -> Objects.equals(d.getTenantId(), tenantId))
			.map(CustomFieldDefinition::getSortOrder)
			.filter(Objects::nonNull)
			.max(Integer::compareTo)
			.orElse(0);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		boolean removed = definitions.removeIf(
			d -> Objects.equals(d.getId(), id) && Objects.equals(d.getTenantId(), tenantId));
		if (!removed) {
			throw new CustomException(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
		}
	}
}
