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
@Table(name = "workplace", uniqueConstraints = {
		@UniqueConstraint(
				name = "UK_WORKPLACE_COMPANY_ID_WORKPLACE_NAME",
				columnNames = {"company_id", "name"}
		)
})
public class JpaWorkplaceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @ToString.Exclude
    private JpaCompanyEntity company;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column(name = "biz_number", length = 10)
    private String bizNumber;

    @Builder.Default
    @OneToMany(mappedBy = "workplace", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<JpaStackEntity> stacks = new ArrayList<>();
}
