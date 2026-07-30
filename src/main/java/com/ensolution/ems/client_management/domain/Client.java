package com.ensolution.ems.client_management.domain;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Client {
    private Long id;
    private Long tenantId;
    private String name;
    private String bizNumber;
    private String representative;
		private String roadAddress;
		private String detailAddress;
		private String zipcode;
    private String manager;
    private String email;
    private String tel;
    private String remark;

		public static Client register(
			Long tenantId,
			String name,
			String bizNumber,
			String representative,
			String roadAddress,
			String detailAddress,
			String zipcode,
			String manager,
			String email,
			String tel
		) {
			return Client.builder()
				.tenantId(tenantId)
				.name(name)
				.bizNumber(bizNumber)
				.representative(representative)
				.roadAddress(roadAddress)
				.detailAddress(detailAddress)
				.zipcode(zipcode)
				.manager(manager)
				.email(email)
				.tel(tel)
				.build();
		}

		public Client update(
			String name,
			String bizNumber,
			String representative,
			String roadAddress,
			String detailAddress,
			String zipcode,
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
				.detailAddress(keep(detailAddress, this.detailAddress))
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
