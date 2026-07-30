package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Client;

import java.util.List;

public interface ClientRepository {
	Client save(Client client);
	Client findById(Long clientId, Long tenantId);
	List<Client> findAll(Long tenantId);
	void deleteById(Long clientId, Long tenantId);

	boolean existsByName(String name);
}
