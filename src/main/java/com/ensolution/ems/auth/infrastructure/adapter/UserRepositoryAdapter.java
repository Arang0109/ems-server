package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.domain.port.UserRepository;
import com.ensolution.ems.auth.infrastructure.entity.UserEntity;
import com.ensolution.ems.auth.infrastructure.repository.RoleJpaRepository;
import com.ensolution.ems.auth.infrastructure.repository.UserJpaRepository;
import com.ensolution.ems.auth.infrastructure.mapper.UserEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

	private final UserJpaRepository repository;
	private final RoleJpaRepository roleJpaRepository;
	private final UserEntityMapper mapper;
	
	@Override
	public User save(User user) {
		UserEntity entity = mapper.toEntity(user).toBuilder()
				.role(roleJpaRepository.getReferenceById(user.getRoleId()))
				.build();
		UserEntity savedEntity = repository.save(entity);
		return mapper.toDomain(savedEntity);
	}
	
	@Override
	public Optional<User> findById(Long id) {
		return repository.findById(id)
				.map(mapper::toDomain);
	}
	
	@Override
	public List<User> findAll(Long tenantId) {
		return mapper.toDomainList(repository.findAllByTenantId(tenantId));
	}
	
	@Override
	public Optional<User> findByUsername(String username) {
		return repository.findByUsername(username).map(mapper::toDomain);
		
	}
	
	@Override
	public boolean existsByUsername(String username) {
		return repository.existsByUsername(username);
	}

	@Override
	public void deleteById(Long id) {
		repository.deleteById(id);
	}
}