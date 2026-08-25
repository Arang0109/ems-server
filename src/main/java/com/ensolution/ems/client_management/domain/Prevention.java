package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Prevention {
	private Long id;
	private Long tenantId;
	private Long stackId;
	private String name;
	private Double capacity;
	private String unit;
	private String targetName;
	private String removalEfficiency;
	private Integer sortOrder;         // 측정지점 안에서의 표시 순서 (성적서에 상위 몇 개만 쓰이므로 업무상 의미가 있다)

	public static Prevention register(Long tenantId, Long stackId, String name, Double capacity, String unit, String targetName, String removalEfficiency, Integer sortOrder) {
		return Prevention.builder().tenantId(tenantId).stackId(stackId).name(name).capacity(capacity).unit(unit).targetName(targetName).removalEfficiency(removalEfficiency).sortOrder(sortOrder).build();
	}

	public Prevention update(String name, Double capacity, String unit, String targetName, String removalEfficiency) {
		return this.toBuilder()
			.name(name)
			.capacity(capacity)
			.unit(unit)
			.targetName(targetName)
			.removalEfficiency(removalEfficiency)
			.build();
	}

	/**
	 * 표시 순서만 바꾼다.
	 * <p>
	 * {@code update()} 는 sortOrder 를 받지 않는다 — 시설 정보 수정과 순서 변경은 별개의 행위이고,
	 * {@code toBuilder()} 가 기존 순서를 그대로 물려주므로 정보 수정으로 순서가 흐트러지지 않는다.
	 */
	public Prevention reorder(Integer sortOrder) {
		return this.toBuilder().sortOrder(sortOrder).build();
	}
}
