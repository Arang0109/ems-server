package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateClientCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateClientCommand;
import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.application.port.out.ClientRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

	private final ClientRepository clientRepository;

	public Client createClient(CreateClientCommand command) {
		if (clientRepository.existsByName(command.name())) { throw new CustomException(ErrorCode.CONFLICT); }

		Client newClient = Client.register(
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.zipcode(),
			command.roadAddress(),
			command.address(),
			command.manager(),
			command.email(),
			command.tel()
		);
		return clientRepository.save(newClient);
	}

	public Client updateClient(Long clientId, UpdateClientCommand command) {
		Client client = clientRepository.findById(clientId);

		Client savedClient = client.update(
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.zipcode(),
			command.roadAddress(),
			command.address(),
			command.manager(),
			command.email(),
			command.tel()
		);

		return clientRepository.save(savedClient);
	}

	public void deleteClient(Long clientId) { clientRepository.deleteById(clientId); }

	@Transactional(readOnly = true)
	public Client getClient(Long clientId) { return clientRepository.findById(clientId); }

	@Transactional(readOnly = true)
	public List<Client> getClientList() { return clientRepository.findAll(); }
}
