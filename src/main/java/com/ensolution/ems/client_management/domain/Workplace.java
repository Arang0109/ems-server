package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.client_management.infrastructure.JpaCompanyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Getter
@Table(name = "workplace")
public class Workplace {
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

    @Column(name = "biz_number", nullable = false, length = 10)
    private String bizNumber;

		@Builder.Default
    @OneToMany(mappedBy = "workplace", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Stack> stacks = new ArrayList<>();
}
