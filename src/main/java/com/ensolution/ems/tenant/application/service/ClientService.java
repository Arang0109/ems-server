package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreateClientCommand;
import com.ensolution.ems.tenant.application.command.update.UpdateClientCommand;
import com.ensolution.ems.tenant.domain.Client;
import com.ensolution.ems.tenant.application.port.out.ClientRepository;
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
