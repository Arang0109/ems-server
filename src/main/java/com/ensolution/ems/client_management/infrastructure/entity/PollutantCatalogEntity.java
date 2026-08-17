package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 고객사에게 지원하는 측정물질 가이드. 이 모듈에서 <b>유일하게 tenant에 종속되지 않는 엔티티</b>로,
 * {@code tenant_id} 컬럼도 {@code TenantEntity} 연관도 갖지 않는다(전 tenant 공유).
 * 조회 시 tenant 조건을 걸지 않는 것이 정상이다.
 *
 * <p>표기명(영문)·시험장비·시험방법은 여기에 두지 않는다 — 고객사가 {@code pollutants}에서 직접 관리한다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "pollutant_catalog",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_pollutant_catalog_field_code",
			columnNames = {"field", "code"}
		)
	},
	indexes = {
		@Index(
			name = "idx_pollutant_catalog_field",
			columnList = "field, sort_order"
		)
	}
)
public class PollutantCatalogEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "catalog_id")
	private Long catalogId;

	/**
	 * 측정분야 안에서 물질을 가리키는 불변 키. 부여 후 변경하지 않는다(클라이언트 분기·스냅샷이 의존).
	 *
	 * <p>같은 물질이라도 대기와 수질은 기준·시험방법이 다른 별개 항목이므로 {@code (field, code)}가 유일하다.
	 * 즉 대기 납과 수질 납이 모두 {@code PB}로 존재할 수 있다.
	 */
	@Column(nullable = false, length = 30)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MeasurementField field;

	/** 가이드 표준 국문명. 고객사가 채택할 때 복사해 가는 초기값이다. */
	@Column(name = "name_kr", nullable = false)
	private String nameKr;

	@Enumerated(EnumType.STRING)
	private MeasurementMethod method;

	@Enumerated(EnumType.STRING)
	private PollutantPhase phase;

	/** 목록 표시 순서(법령 고시 순서). */
	@Column(name = "sort_order")
	private Integer sortOrder;

	/** 폐지 항목은 false. 하드 삭제는 하지 않는다 — 고객사가 채택한 행과 측정계획 스냅샷이 참조한다. */
	@Column(nullable = false)
	private boolean active;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
