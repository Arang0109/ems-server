package com.ensolution.ems.platform.infrastructure.entity;

import com.ensolution.ems.global.common.enums.SubscriptionPlan;
import com.ensolution.ems.global.common.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "tenants")
public class TenantEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tenant_id")
	private Long tenantId;
	
	@Column(nullable = false)
	private String name;
	
	@Column(name = "biz_number", length = 10, unique = true)
	private String bizNumber;
	
	@Column
	private String representative;
	
	@Column(name = "road_address")
	private String roadAddress;
	
	@Column(name = "detail_address")
	private String detailAddress;
	
	@Column
	private String zipcode;
	
	@Enumerated(EnumType.STRING)
	private TenantStatus status;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "subscription_plan")
	private SubscriptionPlan subscriptionPlan;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}