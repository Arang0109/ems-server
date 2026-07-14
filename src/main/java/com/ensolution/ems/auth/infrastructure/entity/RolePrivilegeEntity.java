package com.ensolution.ems.auth.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(
	name = "role_privileges",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_role_privileges",
			columnNames = {"role_id", "privilege_id"}
		)
	},
	indexes = {
		@Index(name = "idx_role_privileges_role_id", columnList = "role_id")
	}
)
public class RolePrivilegeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "role_privilege_id")
	private Long rolePrivilegeId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "role_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_role_privileges_roles")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private RoleEntity role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "privilege_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_role_privileges_privileges")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private PrivilegeEntity privilege;
}
