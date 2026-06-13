package com.ensolution.ems.client_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "target_substance")
public class TargetSubstanceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "prevention_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private PreventionEntity prevention;
	
	@Column(nullable = false)
	private String name;
	
	@Column(name = "removal_efficiency")
	private Double removalEfficiency;
}