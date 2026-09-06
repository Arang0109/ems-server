package com.ensolution.ems.global.security.user;

import com.ensolution.ems.auth.application.port.in.UserCredentialQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserCredentialSummary;
import com.ensolution.ems.platform.application.port.in.TenantQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스프링 시큐리티가 요구하는 인증 주체 조회. <b>두 모듈의 값을 합치는 자리다</b> —
 * 계정은 {@code auth}가, 소속 테넌트 이름은 {@code platform}이 원장을 갖는다.
 * <p>
 * 그 합성을 {@code auth} 안에서 하지 않는 이유는 순환 때문이다. {@code platform}이 이미
 * {@code auth}의 포트(역할 확보·초기 관리자 생성)를 쓰므로, {@code auth}가 {@code platform}을
 * 참조하면 두 모듈이 서로를 향하게 된다. 어느 쪽 모듈도 아닌 여기가 합성 지점으로 알맞다.
 * <p>
 * <b>참조하는 것은 두 모듈의 {@code port/in}뿐이다.</b> 한때 이 클래스가 auth·platform의 JPA
 * 엔티티와 Spring Data 리포지토리를 직접 들고 있었고(루트 {@code CLAUDE.md} 규칙 1·4 위반),
 * 진단 리포트에 세 번 연속 이월된 항목이었다. 편의를 위해 엔티티를 다시 끌어오지 말 것.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private static final String ROLE_PREFIX = "ROLE_";

	private final UserCredentialQueryUseCase userCredentialQueryUseCase;
	private final TenantQueryUseCase tenantQueryUseCase;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		UserCredentialSummary credential = userCredentialQueryUseCase.findCredentialByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found : " + username));

		String tenantName = tenantQueryUseCase.getTenantSummary(credential.tenantId()).name();

		return new CustomUserDetails(
			credential.userId(),
			credential.tenantId(),
			tenantName,
			credential.username(),
			credential.password(),
			credential.name(),
			authoritiesOf(credential.role())
		);
	}

	private static List<GrantedAuthority> authoritiesOf(String role) {
		return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));
	}
}
