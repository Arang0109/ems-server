package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.application.port.out.ClientRepository;
import com.ensolution.ems.client_management.infrastructure.entity.ClientEntity;
import com.ensolution.ems.client_management.infrastructure.repository.ClientJpaRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.ClientEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryAdapter implements ClientRepository {

	private final ClientJpaRepository jpaClientRepository;
	private final ClientEntityMapper mapper;

	@Override
	public Client save(Client client) {
		if (client.getId() != null) {
			ClientEntity existing = jpaClientRepository.findById(client.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			ClientEntity updated = mapper.toEntity(client).toBuilder()
				.workplaces(existing.getWorkplaces())
				.build();
			return mapper.toDomain(jpaClientRepository.save(updated));
		}
		return mapper.toDomain(jpaClientRepository.save(mapper.toEntity(client)));
	}

	@Override
	public Client findById(Long clientId) {
		ClientEntity client = jpaClientRepository.findById(clientId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		return mapper.toDomain(client);
	}

	@Override
	public List<Client> findAll() {
		List<ClientEntity> clients = jpaClientRepository.findAll();
		return mapper.toDomainList(clients);
	}

	@Override
	public void deleteById(Long clientId) { jpaClientRepository.deleteById(clientId); }

	@Override
	public boolean existsByName(String name) { return jpaClientRepository.existsByName(name); }
}
