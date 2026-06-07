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
@Table(name = "stack_pollutant", uniqueConstraints = {
	@UniqueConstraint(
		name = "UK_STACK_POLLUTANT_STACK_ID_POLLUTANT_ID",
		columnNames = {"stack_id", "pollutant_id"}
	)
})
public class JpaStackPollutantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stack_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private JpaStackEntity stack;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pollutant_id", nullable = false)
	private JpaPollutantEntity pollutant;
}
