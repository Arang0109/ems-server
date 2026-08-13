package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(
	name = "workplaces",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_workplaces_client_name",
			columnNames = {"client_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_workplaces_tenant_id",
			columnList = "tenant_id"
		)
	}
)
public class WorkplaceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "workplace_id")
	private Long workplaceId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_workplaces_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "client_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_workplaces_clients")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ClientEntity client;

	@Column(nullable = false)
	private String name;
	
	@Column(name = "biz_number", length = 10)
	private String bizNumber;
	
	@Column(name = "business_category")
	private String businessCategory;
	
	@Column(name = "road_address")
	private String roadAddress;
	
	@Column(name = "detail_address")
	private String detailAddress;
	
	@Column
	private String zipcode;
	
	@Column(name = "facility_manager")
	private String facilityManager;
	
	@Column(name = "sampling_witness")
	private String samplingWitness;

	@Enumerated(EnumType.STRING)
	private Grade grade;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
