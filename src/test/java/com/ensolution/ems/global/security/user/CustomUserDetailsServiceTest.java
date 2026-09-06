package com.ensolution.ems.global.security.user;

import com.ensolution.ems.auth.application.port.in.UserCredentialQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserCredentialSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.application.port.in.TenantQueryUseCase;
import com.ensolution.ems.platform.application.port.in.TenantSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 인증 주체 조립이 지키는 것을 고정한다.
 *
 * <p>첫째, <b>{@code ROLE_} 접두어 부여</b>. 저장된 역할 이름은 {@code ADMIN}이지만 시큐리티가 보는
 * authority는 {@code ROLE_ADMIN}이어야 한다. 이 규칙이 어긋나면 {@code hasRole("ADMIN")}으로 보호된
 * 경로가 통째로 막히거나 열린다 — 컴파일로는 잡히지 않는 종류의 사고다.
 *
 * <p>둘째, <b>없는 계정은 예외가 아니라 {@link UsernameNotFoundException}</b>이라는 것. 이 타입이라야
 * 스프링 시큐리티가 인증 실패로 다루고, 다른 예외면 500이 된다.
 *
 * <p>셋째, <b>두 모듈의 값이 제자리에 꽂히는지</b>. 계정은 {@code auth}, 테넌트 이름은 {@code platform}에서
 * 오며 이 클래스가 합친다. 그 합성을 auth 안에서 하지 않는 이유는 순환 때문이고, 그 사정은
 * 클래스 javadoc에 적혀 있다.
 */
class CustomUserDetailsServiceTest {

	private static final long TENANT = 1L;

	private final StubCredentialQuery credentialQuery = new StubCredentialQuery();
	private final StubTenantQuery tenantQuery = new StubTenantQuery();

	private final CustomUserDetailsService service =
		new CustomUserDetailsService(credentialQuery, tenantQuery);

	@Nested
	@DisplayName("주체 조립")
	class Loading {

		@Test
		@DisplayName("두 모듈의 값이 각자 제자리에 꽂힌다")
		void 계정과_테넌트가_합쳐진다() {
			credentialQuery.given(new UserCredentialSummary(
				7L, TENANT, "member", "encoded-pw", "홍길동", "ADMIN"));
			tenantQuery.given(TENANT, "엔솔루션");

			CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("member");

			assertThat(details.getUserId()).isEqualTo(7L);
			assertThat(details.getTenantId()).isEqualTo(TENANT);
			assertThat(details.getTenant()).isEqualTo("엔솔루션");
			assertThat(details.getUsername()).isEqualTo("member");
			assertThat(details.getPassword()).isEqualTo("encoded-pw");
			assertThat(details.getName()).isEqualTo("홍길동");
		}

		@Test
		@DisplayName("역할 이름에 ROLE_ 접두어가 붙는다 — hasRole 보호가 여기에 달려 있다")
		void 역할에_접두어가_붙는다() {
			credentialQuery.given(new UserCredentialSummary(
				7L, TENANT, "member", "pw", "홍길동", "ADMIN"));
			tenantQuery.given(TENANT, "엔솔루션");

			CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("member");

			assertThat(details.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ADMIN");
		}

		@Test
		@DisplayName("getRole()은 접두어를 뗀 이름을 돌려준다 — 토큰에 실리는 값이다")
		void 대표_역할은_접두어를_뗀다() {
			credentialQuery.given(new UserCredentialSummary(
				7L, TENANT, "ops", "pw", "운영자", "PLATFORM_ADMIN"));
			tenantQuery.given(TENANT, "플랫폼");

			CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("ops");

			assertThat(details.getRole()).isEqualTo("PLATFORM_ADMIN");
		}
	}

	@Nested
	@DisplayName("실패 처리")
	class Failures {

		@Test
		@DisplayName("없는 계정은 UsernameNotFoundException — 다른 예외면 500이 된다")
		void 없는_계정은_인증_실패다() {
			assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
				.isInstanceOf(UsernameNotFoundException.class);
		}

		@Test
		@DisplayName("계정은 있는데 테넌트가 없으면 TENANT_NOT_FOUND가 그대로 올라온다")
		void 테넌트가_없으면_그대로_드러낸다() {
			credentialQuery.given(new UserCredentialSummary(
				7L, 999L, "orphan", "pw", "미아", "USER"));

			assertThatThrownBy(() -> service.loadUserByUsername("orphan"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.TENANT_NOT_FOUND);
		}
	}

	/** 준비된 자격증명을 username으로 돌려주는 {@link UserCredentialQueryUseCase}. */
	private static class StubCredentialQuery implements UserCredentialQueryUseCase {

		private final Map<String, UserCredentialSummary> byUsername = new HashMap<>();

		void given(UserCredentialSummary summary) {
			byUsername.put(summary.username(), summary);
		}

		@Override
		public Optional<UserCredentialSummary> findCredentialByUsername(String username) {
			return Optional.ofNullable(byUsername.get(username));
		}
	}

	/** 미등록 tenantId는 실제 어댑터와 같이 {@code TENANT_NOT_FOUND}를 던진다. */
	private static class StubTenantQuery implements TenantQueryUseCase {

		private final Map<Long, String> nameByTenantId = new HashMap<>();

		void given(Long tenantId, String name) {
			nameByTenantId.put(tenantId, name);
		}

		@Override
		public TenantSummary getTenantSummary(Long tenantId) {
			String name = nameByTenantId.get(tenantId);
			if (name == null) {
				throw new CustomException(ErrorCode.TENANT_NOT_FOUND);
			}
			return new TenantSummary(tenantId, name, null, null, null, null, null, null, null);
		}
	}
}
