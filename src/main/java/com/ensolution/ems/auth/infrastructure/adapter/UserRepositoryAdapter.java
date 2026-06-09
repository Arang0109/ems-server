package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.domain.port.UserRepository;
import com.ensolution.ems.auth.infrastructure.UserEntity;
import com.ensolution.ems.auth.infrastructure.UserJpaRepository;
import com.ensolution.ems.auth.infrastructure.mapper.UserEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
	
	private final UserJpaRepository repository;
	private final UserEntityMapper mapper;
	
	@Override
	public User save(User user) {
		UserEntity savedEntity = repository.save(mapper.toEntity(user));
		return mapper.toDomain(savedEntity);
	}
	
	@Override
	public Optional<User> findById(Long id) {
		return repository.findById(id)
				.map(mapper::toDomain);
	}
	
	@Override
	public Optional<User> findByUsername(String username) {
		return repository.findByUsername(username).map(mapper::toDomain);
		
	}
	
	@Override
	public boolean existsByUsername(String username) {
		return repository.existsByUsername(username);
	}
}