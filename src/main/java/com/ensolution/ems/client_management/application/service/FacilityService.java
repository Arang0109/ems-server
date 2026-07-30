package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateFacilityCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateFacilityCommand;
import com.ensolution.ems.client_management.application.port.out.FacilityRepository;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.domain.Facility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityService {

	private final FacilityRepository facilityRepository;
	private final StackRepository stackRepository;

	public Facility createFacility(CreateFacilityCommand command) {
		stackRepository.findById(command.stackId(), command.tenantId());
		return facilityRepository.save(
			Facility.register(
				command.tenantId(), command.stackId(), command.name(), command.fuelUsage(), command.productOutput(),
				command.incinerationAmount(), command.fuelInput(), command.fuelType(), command.unit())
		);
	}

	public Facility updateFacility(Long facilityId, Long tenantId, UpdateFacilityCommand command) {
		Facility facility = facilityRepository.findById(facilityId, tenantId);
		return facilityRepository.save(
			facility.update(
				command.name(), command.fuelUsage(), command.productOutput(),
				command.incinerationAmount(), command.fuelInput(), command.fuelType(), command.unit())
		);
	}

	public void deleteFacility(Long facilityId, Long tenantId) {
		facilityRepository.deleteById(facilityId, tenantId);
	}

	@Transactional(readOnly = true)
	public Facility getFacility(Long facilityId, Long tenantId) {
		return facilityRepository.findById(facilityId, tenantId);
	}

	@Transactional(readOnly = true)
	public List<Facility> getFacilityList(Long stackId, Long tenantId) {
		return facilityRepository.findByStackId(stackId, tenantId);
	}
}
