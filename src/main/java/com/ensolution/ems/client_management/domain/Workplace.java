package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Workplace {
    private Long id;
    private Long companyId;
    private String name;
    private String address;
    private String bizNumber;

    public static Workplace register(
        Long companyId,
        String name,
        String address,
        String bizNumber
    ) {
        return Workplace.builder()
            .companyId(companyId)
            .name(name)
            .address(address)
            .bizNumber(bizNumber)
            .build();
    }
}
