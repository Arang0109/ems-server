package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.domain.port.RoleRepository;
import com.ensolution.ems.auth.infrastructure.mapper.RoleEntityMapper;
import com.ensolution.ems.auth.infrastructure.repository.RoleJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

	private final RoleJpaRepository repository;
	private final RoleEntityMapper mapper;
	
	@Override
	public List<Role> findAll() {
		return mapper.toDomainList(repository.findAll());
	}
	
	@Override
	public Role findById(Long id) {
		return repository.findById(id)
				.map(mapper::toDomain)
				.orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));
	}

	@Override
	public Role findByName(String name) {
		return repository.findByName(name)
				.map(mapper::toDomain)
				.orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));
	}

	@Override
	public boolean existsByName(String name) {
		return repository.existsByName(name);
	}

	@Override
	public Role save(Role role) {
		return mapper.toDomain(repository.save(mapper.toEntity(role)));
	}
}
