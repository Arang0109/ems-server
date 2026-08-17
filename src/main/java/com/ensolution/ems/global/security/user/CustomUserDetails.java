package com.ensolution.ems.global.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

	private static final String ROLE_PREFIX = "ROLE_";

	/**
	 * 관리자 권한으로 인정하는 authority.
	 * {@code SecurityConfig} 의 롤 계층({@code ROLE_PLATFORM_ADMIN > ROLE_ADMIN})은 접근 결정 시점에만
	 * 적용되고 토큰의 authority 목록에는 반영되지 않으므로, 상위 롤을 여기서 함께 열거합니다.
	 */
	private static final List<String> ADMIN_AUTHORITIES = List.of("ROLE_ADMIN", "ROLE_PLATFORM_ADMIN");

	private final Long userId;
	private final Long tenantId;
	private final String tenant;
	private final String username;
	private final String password;
	private final String name;
	private final Collection<? extends GrantedAuthority> authorities;

	public CustomUserDetails(
		Long userId,
		Long tenantId,
		String tenant,
		String username,
		String password,
		String name,
		Collection<? extends GrantedAuthority> authorities
	) {
		this.userId = userId;
		this.tenantId = tenantId;
		this.tenant = tenant;
		this.username = username;
		this.password = password;
		this.name = name;
		this.authorities = authorities == null ? List.of() : authorities;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	/**
	 * 관리자 권한 보유 여부입니다.
	 * <p>
	 * 경로·메서드 단위 통제({@code @PreAuthorize})로 표현할 수 없는 <b>리소스 상태에 따라 달라지는</b>
	 * 권한 판정에만 사용합니다. 엔드포인트 전체가 관리자 전용이라면 {@code @PreAuthorize} 를 쓰세요.
	 * 예) 측정계획 재개방 — 완료 건은 관리자만, 취소 건은 담당자도 되돌릴 수 있습니다.
	 */
	public boolean hasAdminPrivilege() {
		return authorities.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(ADMIN_AUTHORITIES::contains);
	}

	/**
	 * 대표 롤 이름을 반환합니다. {@code ROLE_} 접두어를 제거한 첫 번째 권한 이름이며,
	 * 권한이 없으면 {@code null} 입니다.
	 */
	public String getRole() {
		return authorities.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(authority -> authority.startsWith(ROLE_PREFIX))
			.map(authority -> authority.substring(ROLE_PREFIX.length()))
			.findFirst()
			.orElse(null);
	}
}
