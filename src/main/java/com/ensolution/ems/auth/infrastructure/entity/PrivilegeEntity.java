package com.ensolution.ems.auth.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "privileges")
public class PrivilegeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "privilege_id")
	private Long privilegeId;
	
	@Column(nullable = false, unique = true, length = 50)
	private String name;
	
	@Column(nullable = false)
	private String description;
}
