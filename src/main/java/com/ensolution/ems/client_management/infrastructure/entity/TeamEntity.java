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

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(
	name = "teams",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_team_name_tenant",
			columnNames = {"tenant_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_teams_tenant_id",
			columnList = "tenant_id"
		),
		// 로그인 시 "이 사용자가 속한 팀" 역조회 및 1인 1팀 검증에 사용
		@Index(
			name = "idx_teams_mentor_user_id",
			columnList = "mentor_user_id"
		),
		@Index(
			name = "idx_teams_mentee_user_id",
			columnList = "mentee_user_id"
		)
	}
)
public class TeamEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "team_id")
	private Long teamId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_teams_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@Column(name = "name", nullable = false)
	private String name;

	// users(auth 모듈) 참조 id — 크로스 모듈이므로 FK 없이 값만 보관
	@Column(name = "mentor_user_id")
	private Long mentorUserId;

	@Column(name = "mentee_user_id")
	private Long menteeUserId;

	// equipment(MongoDB 모듈) 참조 id(ObjectId 문자열) — 크로스 모듈이므로 FK 없이 값만 보관
	@Column(name = "particle_sampler_id")
	private String particleSamplerId;

	@Column(name = "gas_sampler_id")
	private String gasSamplerId;

	@Column(name = "pitot_tube_id")
	private String pitotTubeId;

	@Column(name = "nozzle_id")
	private String nozzleId;

	@CreatedDate
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
