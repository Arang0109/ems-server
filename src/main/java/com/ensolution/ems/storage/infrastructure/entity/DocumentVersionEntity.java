package com.ensolution.ems.storage.infrastructure.entity;

import com.ensolution.ems.storage.domain.StorageProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "document_versions",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_document_versions_document_no",
			columnNames = {"document_id", "version_no"}
		)
	},
	indexes = {
		@Index(
			name = "idx_document_versions_document_id",
			columnList = "document_id"
		)
	}
)
public class DocumentVersionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_version_id")
	private Long documentVersionId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "document_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_document_versions_documents")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DocumentEntity document;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	@Column(name = "original_filename", nullable = false)
	private String originalFilename;

	@Column(name = "storage_key", nullable = false, length = 512)
	private String storageKey;

	@Column
	private Long size;

	@Column(name = "content_type")
	private String contentType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StorageProvider provider;

	@Column(name = "change_note", length = 500)
	private String changeNote;

	@Column(name = "uploaded_by")
	private Long uploadedBy;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
