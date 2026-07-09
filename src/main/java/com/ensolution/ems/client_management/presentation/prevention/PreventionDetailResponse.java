package com.ensolution.ems.client_management.presentation.prevention;

import com.ensolution.ems.client_management.presentation.target_substance.response.TargetSubstanceResponse;

import java.util.List;

public record PreventionDetailResponse(
	Long id,
	Long stackId,
	String name,
	
	List<TargetSubstanceResponse> targets
) {
}