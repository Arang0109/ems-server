package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
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
public class PollutantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Enumerated(EnumType.STRING)
	private MeasurementField field;
	
	@Column(name = "name_kr", nullable = false, unique = true)
	private String nameKr;

	@Column(name = "name_en", unique = true)
	private String nameEn;
	
	@Enumerated(EnumType.STRING)
	private MeasurementMethod method;
	
	@Enumerated(EnumType.STRING)
	private PollutantPhase phase;
	
	private String equipment;
	
	@Column(name = "test_method")
	private String testMethod;
}