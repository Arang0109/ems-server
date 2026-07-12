package com.ensolution.ems.contract.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "contract_table")
public class ContractTableViewEntity {
	@Id
	private Long id;

	@Column(name = "workplace_id")
	private Long workplaceId;
	@Column(name = "contract_name")
	private String contractName;
	@Column(name = "workplace_name")
	private String workplaceName;
	@Column(name = "client_name")
	private String clientName;
	@Column(name = "contract_date")
	private LocalDate contractDate;
	@Column(name = "start_date")
	private LocalDate startDate;
	@Column(name = "completion_date")
	private LocalDate completionDate;
	private String fields;
}
