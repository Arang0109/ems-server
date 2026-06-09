package com.ensolution.ems.client_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "prevention")
public class PreventionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stack_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StackEntity stack;
	
	@Column(nullable = false)
	private String name;
	
	@OneToMany(
		mappedBy = "prevention",
		cascade = CascadeType.ALL,
		orphanRemoval = true
	)
	@Builder.Default
	private List<TargetSubstanceEntity> targetSubstances = new ArrayList<>();
}