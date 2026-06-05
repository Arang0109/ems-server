package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.domain.port.StackRepository;
import com.ensolution.ems.client_management.infrastructure.entity.JpaStackEntity;
import com.ensolution.ems.client_management.infrastructure.repository.JpaStackRepository;
import com.ensolution.ems.client_management.infrastructure.repository.JpaWorkplaceRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.StackDomainEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class StackRepositoryAdapter implements StackRepository {

    private final JpaStackRepository jpaStackRepository;
    private final JpaWorkplaceRepository jpaWorkplaceRepository;
    private final StackDomainEntityMapper mapper;

    @Override
    public Stack save(Stack stack) {
        JpaStackEntity entity = mapper.toEntity(stack)
            .toBuilder()
            .workplace(jpaWorkplaceRepository.getReferenceById(stack.getWorkplaceId()))
            .build();
        return mapper.toDomain(jpaStackRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Stack findById(Long id) {
        return jpaStackRepository.findById(id)
            .map(mapper::toDomain)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StackListItem> findByWorkplaceId(Long workplaceId) {
        return mapper.toStackListItems(jpaStackRepository.findByWorkplaceId(workplaceId));
    }

    @Override
    public void deleteById(Long id) {
        jpaStackRepository.deleteById(id);
    }
}
