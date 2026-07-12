package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.Grade;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "workplace", uniqueConstraints = {
	@UniqueConstraint(
		name = "UK_WORKPLACE_CLIENT_ID_WORKPLACE_NAME",
		columnNames = {"client_id", "name"}
	)
})
public class WorkplaceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id")
	@ToString.Exclude
	private ClientEntity client;

	@Column(nullable = false)
	private String name;

	@Column
	private String zipcode;

	@Column(name = "road_address")
	private String roadAddress;

	@Column
	private String address;

	@Column(name = "biz_number", length = 10)
	private String bizNumber;

	@Enumerated(EnumType.STRING)
	private Grade grade;

	@Builder.Default
	@OneToMany(mappedBy = "workplace", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<StackEntity> stacks = new ArrayList<>();
}
