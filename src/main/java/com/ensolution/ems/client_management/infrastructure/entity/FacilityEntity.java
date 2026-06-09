package com.ensolution.ems.client_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "facility")
public class FacilityEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stack_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StackEntity stack;
	
	@Column(nullable = false) private String name;
	@Column(name = "fuel_usage") private String fuelUsage;
	@Column(name = "fuel_input") private String fuelInput;
	@Column(name = "fuel_type") private String fuelType;
}