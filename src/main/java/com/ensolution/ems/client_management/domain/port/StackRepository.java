package com.ensolution.ems.client_management.domain.port;

import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;

public interface StackRepository {
    Stack save(Stack stack);
    Stack findById(Long id);
    List<StackListItem> findByWorkplaceId(Long workplaceId);
    void deleteById(Long id);
		boolean existsByNameAndWorkplaceIdAndField(String name, Long workplaceId, MeasurementField field);
}
