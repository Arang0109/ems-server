package com.ensolution.ems.storage.infrastructure.entity;

import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import com.ensolution.ems.global.common.enums.DocumentCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "documents",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_documents_tenant_name",
			columnNames = {"tenant_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_documents_tenant_id",
			columnList = "tenant_id"
		)
	}
)
public class DocumentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_id")
	private Long documentId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_documents_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DocumentCategory category;

	@Column(length = 500)
	private String description;

	@Column(name = "latest_version_no", nullable = false)
	private int latestVersionNo;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
