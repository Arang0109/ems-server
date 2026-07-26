package com.ensolution.ems.contract.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.time.LocalDate;

@Builder(toBuilder = true)
@Immutable
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Subselect("""
		SELECT c.contract_id, c.tenant_id, c.workplace_id, c.contract_name,
		       c.contract_date, c.start_date, c.completion_date,
		       w.name AS workplace_name, cl.name AS client_name,
		       COALESCE((SELECT GROUP_CONCAT(DISTINCT s.field ORDER BY s.field SEPARATOR ',')
		                   FROM stacks s WHERE s.workplace_id = c.workplace_id), '') AS fields
		FROM contracts c
		JOIN workplaces w ON w.workplace_id = c.workplace_id
		JOIN clients cl   ON cl.client_id   = w.client_id
		""")
@Synchronize({"contracts", "workplaces", "clients", "stacks"})
public class ContractTableViewEntity {
	@Id
	@Column(name = "contract_id")
	private Long contractId;

	@Column(name = "workplace_id")
	private Long workplaceId;
	@Column(name = "tenant_id")
	private Long tenantId;
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
