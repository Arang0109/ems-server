package com.ensolution.ems.client_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "company")
public class CompanyEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String name;
	
	@Column(name = "biz_number", length = 10)
	private String bizNumber;
	
	@Column
	private String representative;
	
	@Column
	private String address;
	
	@Column
	private String manager;
	
	@Column
	private String email;
	
	@Column
	private String tel;
	
	@Column(columnDefinition = "LONGTEXT")
	private String remark;
	
	@Builder.Default
	@OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<WorkplaceEntity> workplaces = new ArrayList<>();
}