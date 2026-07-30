package com.ensolution.ems.platform.infrastructure.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 운영자(PLATFORM_ADMIN) 부트스트랩 설정. 값은 환경변수로 주입한다.
 * (PLATFORM_ADMIN_USERNAME, PLATFORM_ADMIN_PASSWORD 등)
 */
@Component
@ConfigurationProperties(prefix = "platform.bootstrap")
@Getter
@Setter
public class PlatformBootstrapProperties {

	/** 부트스트랩 활성화 여부. false거나 username/password 미설정 시 아무 작업도 하지 않는다. */
	private boolean enabled = false;

	private String username;

	private String password;

	private String name;

	private String department;

	private String email;

	private String tel;

	private String systemTenantName = "플랫폼";

	private String systemTenantBizNumber = "0000000000";
}
