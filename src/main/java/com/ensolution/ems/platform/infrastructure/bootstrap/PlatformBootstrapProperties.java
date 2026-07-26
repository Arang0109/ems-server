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

	/** 운영자 계정 아이디 */
	private String username;

	/** 운영자 계정 초기 비밀번호(평문, 저장 시 BCrypt 인코딩) */
	private String password;

	/** 운영자 이름 */
	private String name = "운영자";

	private String department;

	private String email;

	private String tel;

	/** 운영자가 소속되는 전용 시스템 테넌트명 */
	private String systemTenantName = "플랫폼";

	/** 시스템 테넌트 식별용 예약 사업자번호(10자리). 실제 고객사와 충돌하지 않는 값. */
	private String systemTenantBizNumber = "0000000000";
}
