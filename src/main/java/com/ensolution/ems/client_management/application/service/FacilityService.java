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
		stackRepository.findById(command.stackId());
		return facilityRepository.save(
			Facility.register(command.stackId(), command.name(), command.fuelUsage(), command.fuelInput(), command.fuelType())
		);
	}

	public Facility updateFacility(Long facilityId, UpdateFacilityCommand command) {
		Facility facility = facilityRepository.findById(facilityId);
		return facilityRepository.save(
			facility.update(command.name(), command.fuelUsage(), command.fuelInput(), command.fuelType())
		);
	}

	public void deleteFacility(Long facilityId) {
		facilityRepository.deleteById(facilityId);
	}

	@Transactional(readOnly = true)
	public Facility getFacility(Long facilityId) {
		return facilityRepository.findById(facilityId);
	}

	@Transactional(readOnly = true)
	public List<Facility> getFacilityList(Long stackId) {
		return facilityRepository.findByStackId(stackId);
	}
}
