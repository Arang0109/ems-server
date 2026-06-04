package com.ensolution.ems.client_management.domain.port;

import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;

import java.util.List;
import java.util.Optional;

public interface StackRepository {
    Stack save(Stack stack);
    Optional<Stack> findById(Long id);
    List<StackListItem> findByWorkplaceId(Long workplaceId);
    void deleteById(Long id);
}
