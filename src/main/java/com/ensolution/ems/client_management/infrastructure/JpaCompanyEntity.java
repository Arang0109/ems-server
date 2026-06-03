package com.ensolution.ems.client_management.infrastructure;

import com.ensolution.ems.client_management.domain.Workplace;
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
public class JpaCompanyEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 100)
	private String name;
	
	@Column(name = "biz_number", length = 10)
	private String bizNumber;
	
	@Column(length = 100)
	private String representative;
	
	@Column
	private String address;
	
	@Column(length = 100)
	private String manager;
	
	@Column(length = 100)
	private String email;
	
	@Column(length = 100)
	private String tel;
	
	@Column(columnDefinition = "LONGTEXT")
	private String remark;
	
	@Builder.Default
	@OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<Workplace> workplaces = new ArrayList<>();
}