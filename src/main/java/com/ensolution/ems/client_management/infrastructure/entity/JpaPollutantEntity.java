package com.ensolution.ems.client_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "pollutant")
public class JpaPollutantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "measurement_")
	
	@Column(name = "name_kr", nullable = false, unique = true)
	private String nameKr;

	@Column(name = "name_en", unique = true)
	private String nameEn;
}
