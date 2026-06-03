package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Company {
    private Long id;
    private String name;
    private String bizNumber;
    private String representative;
    private String address;
    private String manager;
    private String email;
    private String tel;
    private String remark;
		
		public static Company register(
			String name,
			String bizNumber,
			String representative,
			String address,
			String manager,
			String email,
			String tel
		) {
			return Company.builder()
				.name(name)
				.bizNumber(bizNumber)
				.representative(representative)
				.address(address)
				.manager(manager)
				.email(email)
				.tel(tel)
				.build();
		}
		
		
}