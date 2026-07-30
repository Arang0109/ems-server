package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateClientCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateClientCommand;
import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.application.port.out.ClientRepository;
import com.ensolution.ems.client_management.application.validator.ClientValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

	private final ClientRepository clientRepository;
	private final ClientValidator clientValidator;

	public Client createClient(CreateClientCommand command) {
		clientValidator.requireUniqueName(command.name());

		Client newClient = Client.register(
			command.tenantId(),
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.roadAddress(),
			command.detailAddress(),
			command.zipcode(),
			command.manager(),
			command.email(),
			command.tel()
		);
		return clientRepository.save(newClient);
	}

	public Client updateClient(Long clientId, Long tenantId, UpdateClientCommand command) {
		Client client = clientRepository.findById(clientId, tenantId);

		Client savedClient = client.update(
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.roadAddress(),
			command.detailAddress(),
			command.zipcode(),
			command.manager(),
			command.email(),
			command.tel()
		);

		return clientRepository.save(savedClient);
	}

	public void deleteClient(Long clientId, Long tenantId) { clientRepository.deleteById(clientId, tenantId); }

	@Transactional(readOnly = true)
	public Client getClient(Long clientId, Long tenantId) { return clientRepository.findById(clientId, tenantId); }

	@Transactional(readOnly = true)
	public List<Client> getClientList(Long tenantId) { return clientRepository.findAll(tenantId); }
}
