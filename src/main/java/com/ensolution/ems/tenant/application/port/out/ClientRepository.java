package com.ensolution.ems.tenant.application.port.out;

import com.ensolution.ems.tenant.domain.Client;

import java.util.List;

public interface ClientRepository {
	Client save(Client client);
	Client findById(Long clientId, Long tenantId);
	List<Client> findAll(Long tenantId);
	void deleteById(Long clientId, Long tenantId);

	boolean existsByName(String name);
}
