package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;
import java.util.Map;

public interface StackRepository {
    Stack save(Stack stack);
    Stack findById(Long id);
		List<StackListItem> findAll();
    List<StackListItem> findByWorkplaceId(Long workplaceId);
    Map<Long, List<MeasurementField>> findFieldsByWorkplaceIds(List<Long> workplaceIds);
    void deleteById(Long id);
    boolean existsByNameAndWorkplaceIdAndField(String name, Long workplaceId, MeasurementField field);
}
