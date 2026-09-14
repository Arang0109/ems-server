package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 고객사가 가이드({@code pollutant_catalog})에서 채택한 측정물질.
 * 가이드에 없는 물질은 만들 수 없으므로 {@code catalog_id}는 NOT NULL이다.
 *
 * <p>여기 있는 컬럼은 전부 <b>고객사 소유값</b>이다. {@code field}·{@code phase}는
 * 카탈로그가 단일 진실 소스이므로 컬럼으로 두지 않고 조회 시 조인으로 채운다
 * ({@code PollutantEntityMapper.toDomain}). 측정방법은 {@code method_id}로 참조하며, 채취 단위·채취시간
 * 같은 측정방법 속성도 같은 이유로 컬럼이 아니라 조인 투영이다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "pollutants",
	uniqueConstraints = {
		/*
		 * 물질의 중복 채택은 이 제약 하나로 막는다.
		 * (tenant_id, name_kr) 유니크는 둘 수 없다 — 채택 시 카탈로그 국문명을 복사하므로
		 * 한 고객사가 분야가 다른 동명 물질(대기 납·수질 납)을 함께 쓰면 정상 요청이 거부된다.
		 */
		@UniqueConstraint(
			name = "uk_pollutants_tenant_catalog",
			columnNames = {"tenant_id", "catalog_id"}
		)
	},
	indexes = {
		@Index(
			name = "idx_pollutants_tenant_id",
			columnList = "tenant_id"
		)
	}

)
public class PollutantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pollutant_id")
	private Long pollutantId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_pollutants_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	/**
	 * 채택한 가이드 항목. 고객사는 가이드 밖의 물질을 만들 수 없으므로 필수다.
	 * 카탈로그는 하드 삭제하지 않고 {@code active=false}로만 폐지하므로 {@code @OnDelete}를 두지 않는다.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "catalog_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_pollutants_catalog")
	)
	private PollutantCatalogEntity catalog;

	/**
	 * 고객사가 채택 시 정하는 측정방법. 같은 카탈로그 항목이라도 업체마다 다를 수 있어 카탈로그가 아니라
	 * 여기서 정한다. API는 필수지만 백필되지 못한 레거시 행 호환을 위해 DB는 nullable이다.
	 * 삭제는 {@code MeasurementMethodValidator.requireNotReferenced}가 막으므로 {@code @OnDelete}를 두지 않는다.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "method_id",
		foreignKey = @ForeignKey(name = "fk_pollutants_measurement_methods")
	)
	private MeasurementMethodEntity method;

	/**
	 * 항목별 채취시간 오버라이드(분). null이면 측정방법의 기본값을 따른다.
	 * 흡수액처럼 항목마다 따로 잡는 방법은 물질마다 흡인 시간이 다를 수 있어 둔다. 통칭 채취(MERGED) 항목은 서비스가 막는다.
	 */
	@Column(name = "sampling_minutes")
	private Integer samplingMinutes;

	/**
	 * 항목별 흡인유량 오버라이드(L/min). null이면 측정방법의 기본값을 따른다. 규칙은 sampling_minutes 와 같다.
	 */
	@Column(name = "suction_flow_rate", precision = 10, scale = 3)
	private BigDecimal suctionFlowRate;

	/** 채택 시 카탈로그 국문명을 복사하므로 항상 값이 있다. 이후 수정은 고객사 몫이다. */
	@Column(name = "name_kr", nullable = false)
	private String nameKr;

	@Column(name = "name_en")
	private String nameEn;

	private String equipment;

	@Column(name = "test_method")
	private String testMethod;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
