package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.port.out.FacilityRepository;
import com.ensolution.ems.client_management.domain.Facility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityQueryService {

	private final FacilityRepository facilityRepository;

	public Facility getFacility(Long facilityId) {
		return facilityRepository.findById(facilityId);
	}

	public List<Facility> getFacilityList(Long stackId) {
		return facilityRepository.findByStackId(stackId);
	}
}
