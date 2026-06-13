package com.ensolution.ems.client_management.domain;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Company {
    private Long id;
    private String name;
    private String bizNumber;
    private String representative;
		private String zipcode;
		private String roadAddress;
    private String address;
    private String manager;
    private String email;
    private String tel;
    private String remark;
		
		public static Company register(
			String name,
			String bizNumber,
			String representative,
			String zipcode,
			String roadAddress,
			String address,
			String manager,
			String email,
			String tel
		) {
			return Company.builder()
				.name(name)
				.bizNumber(bizNumber)
				.representative(representative)
				.zipcode(zipcode)
				.roadAddress(roadAddress)
				.address(address)
				.manager(manager)
				.email(email)
				.tel(tel)
				.build();
		}
	
		public Company update(
			String name,
			String bizNumber,
			String representative,
			String zipcode,
			String roadAddress,
			String address,
			String manager,
			String email,
			String tel
		) {
			return this.toBuilder()
				.name(keep(name, this.name))
				.bizNumber(keep(bizNumber, this.bizNumber))
				.representative(keep(representative, this.representative))
				.zipcode(keep(zipcode, this.zipcode))
				.roadAddress(keep(roadAddress, this.roadAddress))
				.address(keep(address, this.address))
				.manager(keep(manager, this.manager))
				.email(keep(email, this.email))
				.tel(keep(tel, this.tel))
				.build();
		}
	
	private static String keep(String value, String original) {
		return value == null || value.isBlank()
			? original
			: value;
	}
}