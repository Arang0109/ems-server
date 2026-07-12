package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Client;

import java.util.List;

public interface ClientRepository {
	Client save(Client client);
	Client findById(Long clientId);
	List<Client> findAll();
	void deleteById(Long clientId);

	boolean existsByName(String name);
}
