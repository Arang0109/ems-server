package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.FakeRoleRepository;
import com.ensolution.ems.auth.application.FakeUserRepository;
import com.ensolution.ems.auth.application.command.SignInCommand;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.application.port.out.Authenticator;
import com.ensolution.ems.auth.application.port.out.PasswordEncryptor;
import com.ensolution.ems.auth.application.port.out.TokenIssuer;
import com.ensolution.ems.auth.application.validator.UserValidator;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.TokenResult;
import com.ensolution.ems.client_management.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.TeamSummary;
import com.ensolution.ems.client_management.application.port.in.UserTeamSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그인 응답이 클라이언트에게 <b>무엇을 반드시 넘겨야 하는지</b>를 고정한다.
 *
 * <p>핵심은 <b>{@code userId}가 끝까지 도달하는가</b>이다. 이 값은
 * {@code AuthenticatedUser} → {@code TokenResult} → {@code SignInResult} → {@code SignInResponse}
 * 네 타입을 통과하는데, 원래 {@code AuthenticatedUser}까지는 있으면서 {@code TokenResult}에서
 * <b>조용히 떨어져 있었다.</b> 컴파일도 통과하고 기존 테스트도 전부 통과했다 — 값이 사라진 것을
 * 알아챈 것은 프론트가 붙으려 할 때였다.
 *
 * <p>클라이언트가 자기 PK를 모르면 채팅의 {@code senderId}·{@code readerId}가 내 것인지
 * 판별할 수 없다. {@code username}으로는 대신할 수 없고(동명이인이 아니라 타입이 다르다),
 * 액세스 토큰 claim 에도 없다. 그래서 이 통과 경로 자체를 회귀로 고정한다.
 *
 * <p>쓰기 유스케이스는 {@link AuthServiceTest}가 맡는다. 그쪽은 로그인 협력자를 "호출되면 실패하는"
 * 구현으로 넘겨 <b>그 경로가 저기까지 가지 않는다</b>는 것을 구조로 고정하고 있으므로,
 * 동작하는 스텁이 필요한 이 테스트를 그 안에 넣으면 그 성질이 사라진다.
 */
class AuthServiceSignInTest {

	private static final long TENANT = 1L;
	private static final long USER_ID = 7L;

	private final FakeUserRepository userRepository = new FakeUserRepository();
	private final FakeRoleRepository roleRepository = new FakeRoleRepository();

	private final StubAuthenticator authenticator = new StubAuthenticator();
	private final StubTeamQuery teamQuery = new StubTeamQuery();

	private final AuthService authService = new AuthService(
		userRepository,
		UNUSED_ENCRYPTOR,
		authenticator,
		new StubTokenIssuer(),
		teamQuery,
		new UserValidator(roleRepository)
	);

	private SignInResult signIn() {
		return authService.signIn(new SignInCommand("member", "raw-password"));
	}

	@Test
	@DisplayName("사용자 PK가 응답까지 도달한다 — 채팅이 내 메시지를 구분하는 유일한 근거다")
	void 사용자_pk가_응답에_실린다() {
		assertThat(signIn().userId()).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("토큰과 신원이 함께 실린다")
	void 토큰과_신원이_실린다() {
		SignInResult result = signIn();

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(result.tenantId()).isEqualTo(TENANT);
		assertThat(result.tenant()).isEqualTo("엔솔루션");
		assertThat(result.username()).isEqualTo("member");
		assertThat(result.name()).isEqualTo("홍길동");
		assertThat(result.role()).isEqualTo("USER");
	}

	@Test
	@DisplayName("소속 팀이 실린다")
	void 소속_팀이_실린다() {
		teamQuery.given(new UserTeamSummary(3L, "측정1팀"));

		SignInResult result = signIn();

		assertThat(result.teamId()).isEqualTo(3L);
		assertThat(result.teamName()).isEqualTo("측정1팀");
	}

	@Test
	@DisplayName("팀 미배정이어도 로그인은 된다 — 관리자·운영자는 팀이 없는 것이 정상이다")
	void 팀이_없어도_로그인된다() {
		teamQuery.given(null);

		SignInResult result = signIn();

		assertThat(result.userId()).isEqualTo(USER_ID);
		assertThat(result.teamId()).isNull();
		assertThat(result.teamName()).isNull();
	}

	/** 로그인 경로는 비밀번호를 직접 다루지 않는다 — 대조는 {@link Authenticator}의 몫이다. */
	private static final PasswordEncryptor UNUSED_ENCRYPTOR = new PasswordEncryptor() {
		@Override
		public String encode(String rawPassword) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean matches(String rawPassword, String encodedPassword) {
			throw new UnsupportedOperationException();
		}
	};

	/** 인증에 성공한 사용자를 돌려준다. <b>여기에는 userId 가 있다</b> — 문제는 그 뒤 경로였다. */
	private static class StubAuthenticator implements Authenticator {

		@Override
		public AuthenticatedUser authenticate(String username, String password) {
			return new AuthenticatedUser(USER_ID, TENANT, "엔솔루션", username, "홍길동", "USER");
		}

		@Override
		public Optional<AuthenticatedUser> loadAuthenticatedUser(String username) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * 받은 {@link AuthenticatedUser}의 값을 그대로 옮겨 담는다.
	 * <b>여기서 {@code userId}를 빠뜨리는 것이 원래의 결함이었으므로</b>, 스텁도 실제 어댑터처럼
	 * 전부 옮긴다 — 스텁이 대신 채워 주면 서비스의 통과 여부를 검증할 수 없다.
	 */
	private static class StubTokenIssuer implements TokenIssuer {

		@Override
		public TokenResult issue(AuthenticatedUser user) {
			return new TokenResult(
				user.userId(), "access-token", "refresh-token",
				user.tenantId(), user.tenant(), user.username(), user.name(), user.role(),
				25_200_000L);
		}

		@Override
		public String issueAccessToken(AuthenticatedUser user) {
			throw new UnsupportedOperationException();
		}
	}

	/** 팀 조회. 미배정을 null 로 답하는 것이 포트 규약이다. */
	private static class StubTeamQuery implements TeamQueryUseCase {

		private UserTeamSummary team = new UserTeamSummary(3L, "측정1팀");

		void given(UserTeamSummary team) {
			this.team = team;
		}

		@Override
		public TeamSummary getTeamSummary(Long teamId, Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public UserTeamSummary getUserTeamSummary(Long userId, Long tenantId) {
			return team;
		}
	}
}
