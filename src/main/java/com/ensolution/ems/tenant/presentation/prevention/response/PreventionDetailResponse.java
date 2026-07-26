package com.ensolution.ems.tenant.presentation.prevention.response;

import com.ensolution.ems.tenant.presentation.target_substance.response.TargetSubstanceResponse;

import java.util.List;

public record PreventionDetailResponse(
	Long id,
	Long stackId,
	String name,
	Double capacity,

	List<TargetSubstanceResponse> targets
) {
}
