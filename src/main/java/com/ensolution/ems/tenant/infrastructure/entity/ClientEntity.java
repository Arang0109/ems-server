package com.ensolution.ems.tenant.infrastructure.entity;

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
	name = "clients",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_clients_tenants",
			columnNames = {"tenant_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_clients_tenant_id",
			columnList = "tenant_id"
		)
	}
)
public class ClientEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "client_id")
	private Long clientId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_clients_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@Column(nullable = false)
	private String name;

	@Column(name = "biz_number", length = 10)
	private String bizNumber;

	@Column
	private String representative;

	@Column(name = "road_address")
	private String roadAddress;
	
	@Column(name = "detail_address")
	private String detailAddress;
	
	@Column
	private String zipcode;

	@Column
	private String manager;

	@Column
	private String email;

	@Column
	private String tel;

	@Column(columnDefinition = "LONGTEXT")
	private String remark;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
