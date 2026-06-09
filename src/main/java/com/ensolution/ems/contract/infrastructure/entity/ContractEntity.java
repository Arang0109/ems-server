package com.ensolution.ems.contract.infrastructure.entity;

import com.ensolution.ems.contract.domain.ContractAmountUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "contract")
public class ContractEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "workplace_id", nullable = false)
	private Long workplaceId;

	@Column(name = "contract_name")
	private String contractName;
	
	@Column(name = "contract_date")
	private LocalDate contractDate;
	
	@Column(name = "start_date")
	private LocalDate startDate;
	
	@Column(name = "completion_date")
	private LocalDate completionDate;
	
	@Column(name = "contract_amount")
	private BigDecimal contractAmount;
	
	@Column(name = "contract_amount_unit")
	private ContractAmountUnit contractAmountUnit;
	
	@Column(name = "vat_included")
	private boolean vatIncluded;
	
	@Column(name = "contract_guarantee_amount")
	private BigDecimal contractGuaranteeAmount;
	
	@Column(name = "advance_payment_amount")
	private BigDecimal advancePaymentAmount;
	
	@Column(name = "advance_payment_due_date")
	private int advancePaymentDueDate;
	
	@Column(name = "delay_penalty_rate")
	private int delayPenaltyRate;
	
	@Column
	private String remark;
}