package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Facility {
	private Long id;
	private Long tenantId;
	private Long stackId;
	private String name;
	private String fuelUsage;          // 연료 사용량
	private String productOutput;      // 제품 생산량
	private String incinerationAmount; // 소각량
	private String fuelInput;          // 원료 투입량
	private String fuelType;           // 연료·원료 종류
	private String unit;               // 사용량·생산량에 적용되는 단위
	private Integer sortOrder;         // 측정지점 안에서의 표시 순서 (성적서에 상위 몇 개만 쓰이므로 업무상 의미가 있다)

	public static Facility register(
		Long tenantId, Long stackId, String name, String fuelUsage, String productOutput,
		String incinerationAmount, String fuelInput, String fuelType, String unit, Integer sortOrder) {
		return Facility.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.name(name)
			.fuelUsage(fuelUsage)
			.productOutput(productOutput)
			.incinerationAmount(incinerationAmount)
			.fuelInput(fuelInput)
			.fuelType(fuelType)
			.unit(unit)
			.sortOrder(sortOrder)
			.build();
	}

	public Facility update(
		String name, String fuelUsage, String productOutput,
		String incinerationAmount, String fuelInput, String fuelType, String unit) {
		return this.toBuilder()
			.name(name)
			.fuelUsage(fuelUsage)
			.productOutput(productOutput)
			.incinerationAmount(incinerationAmount)
			.fuelInput(fuelInput)
			.fuelType(fuelType)
			.unit(unit)
			.build();
	}

	/**
	 * 표시 순서만 바꾼다.
	 * <p>
	 * {@code update()} 는 sortOrder 를 받지 않는다 — 시설 정보 수정과 순서 변경은 별개의 행위이고,
	 * {@code toBuilder()} 가 기존 순서를 그대로 물려주므로 정보 수정으로 순서가 흐트러지지 않는다.
	 */
	public Facility reorder(Integer sortOrder) {
		return this.toBuilder().sortOrder(sortOrder).build();
	}
}
