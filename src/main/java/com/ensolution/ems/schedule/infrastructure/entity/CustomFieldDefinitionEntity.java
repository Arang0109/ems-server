package com.ensolution.ems.schedule.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 커스텀 필드 정의 엔티티. tenant는 다른 모듈 소유이므로 FK 없이 plain id 컬럼으로만 보관한다
 * ({@code ScheduleEntity}와 동일). 값은 이 테이블이 아니라 Mongo 문서({@code schedule_documents.customFields})에 있다.
 * <p>
 * 컬럼명이 {@code field_key}인 것은 {@code key}가 MySQL 예약어이기 때문이며, 도메인·JSON에서는 {@code key}다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
	name = "schedule_custom_fields",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_schedule_custom_fields_tenant_key",
			columnNames = {"tenant_id", "field_key"}
		)
	},
	indexes = {
		@Index(name = "idx_schedule_custom_fields_tenant_id", columnList = "tenant_id")
	}
)
@EntityListeners(AuditingEntityListener.class)
public class CustomFieldDefinitionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "field_id")
	private Long fieldId;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "field_key", nullable = false, length = 50)
	private String key;

	@Column(nullable = false, length = 100)
	private String label;

	@Column(name = "sort_order")
	private Integer sortOrder;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
