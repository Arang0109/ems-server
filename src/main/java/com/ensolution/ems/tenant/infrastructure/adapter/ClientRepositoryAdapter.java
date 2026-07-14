package com.ensolution.ems.tenant.infrastructure.adapter;

import com.ensolution.ems.tenant.domain.Client;
import com.ensolution.ems.tenant.application.port.out.ClientRepository;
import com.ensolution.ems.tenant.infrastructure.entity.ClientEntity;
import com.ensolution.ems.tenant.infrastructure.repository.ClientJpaRepository;
import com.ensolution.ems.tenant.infrastructure.repository.TenantJpaRepository;
import com.ensolution.ems.tenant.infrastructure.mapper.ClientEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryAdapter implements ClientRepository {

	private final ClientJpaRepository jpaClientRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final ClientEntityMapper mapper;

	@Override
	public Client save(Client client) {
		if (client.getId() != null) {
			jpaClientRepository.findById(client.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		}
		ClientEntity entity = mapper.toEntity(client).toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(client.getTenantId()))
			.build();
		return mapper.toDomain(jpaClientRepository.save(entity));
	}

	@Override
	public Client findById(Long clientId, Long tenantId) {
		ClientEntity client = jpaClientRepository.findByClientIdAndTenant_TenantId(clientId, tenantId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		return mapper.toDomain(client);
	}

	@Override
	public List<Client> findAll(Long tenantId) {
		List<ClientEntity> clients = jpaClientRepository.findAllByTenantTenantId(tenantId);
		return mapper.toDomainList(clients);
	}

	@Override
	public void deleteById(Long clientId, Long tenantId) {
		int deletedCount = jpaClientRepository.deleteByClientIdAndTenantId(clientId, tenantId);
		
		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}

	@Override
	public boolean existsByName(String name) { return jpaClientRepository.existsByName(name); }
}
