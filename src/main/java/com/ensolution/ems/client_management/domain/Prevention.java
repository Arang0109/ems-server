package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Prevention {
	private Long id;
	private Long stackId;
	private String name;
	
	@Builder.Default
	private List<TargetSubstance> targets = new ArrayList<>();
	
	public static Prevention register(Long stackId, String name) {
		return Prevention.builder().stackId(stackId).name(name).build();
	}
	
	public Prevention update(String name) {
		return this.toBuilder().name(name).build();
	}
	
	public void addTarget(TargetSubstance target) {
		this.targets.add(target);
	}
	
	public void removeTarget(Long targetId) {
		TargetSubstance targetSubstance = findTargetSubstance(targetId);
		this.targets.remove(targetSubstance);
	}
	
	private TargetSubstance findTargetSubstance(Long targetId) {
		return this.targets.stream()
			.filter((p) -> p.getId().equals(targetId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}
	
	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}