package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.FakeRoleRepository;
import com.ensolution.ems.auth.application.FakeUserRepository;
import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.UpdateUserCommand;
import com.ensolution.ems.auth.application.validator.UserValidator;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.domain.TokenResult;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.application.port.out.Authenticator;
import com.ensolution.ems.auth.application.port.out.PasswordEncryptor;
import com.ensolution.ems.auth.application.port.out.TokenIssuer;
import com.ensolution.ems.client_management.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.TeamSummary;
import com.ensolution.ems.client_management.application.port.in.UserTeamSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사용자 쓰기 유스케이스가 지키는 세 가지를 고정한다.
 *
 * <p>첫째, <b>역할 부여 제한이 생성·수정 양쪽 경로에 모두 걸려 있다</b>는 것. 한쪽만 막으면
 * "생성은 USER로 해 두고 수정으로 PLATFORM_ADMIN을 넣는" 우회가 열린다. 규칙 13이 "생성·수정
 * 양쪽 경로 모두에 적용된다"고 못 박은 지점이며, 검증 호출을 지워도 컴파일은 통과하므로
 * 테스트가 아니면 회귀를 잡을 수단이 없다.
 *
 * <p>둘째, <b>{@code createPlatformAdmin}이 그 제한을 적용하지 않는 유일한 경로</b>라는 것.
 * 이것은 버그가 아니라 부트스트랩을 위한 의도된 예외다. 다만 "유일한"이 유지되는지는
 * 사람이 지켜야 하므로, 예외가 존재한다는 사실 자체를 테스트로 드러내 둔다.
 *
 * <p>셋째, <b>교차 테넌트 차단</b> — 수정·삭제가 남의 테넌트 사용자에게 닿지 않으며
 * 403이 아니라 {@code USER_NOT_FOUND}로 존재를 은닉한다.
 */
class AuthServiceTest {

	private static final long TENANT = 1L;
	private static final long OTHER_TENANT = 2L;

	private static final long ADMIN_ROLE_ID = 1L;
	private static final long USER_ROLE_ID = 2L;
	private static final long PLATFORM_ADMIN_ROLE_ID = 99L;

	private final FakeUserRepository userRepository = new FakeUserRepository();
	private final FakeRoleRepository roleRepository = new FakeRoleRepository();

	private final AuthService authService = new AuthService(
		userRepository,
		FAKE_ENCRYPTOR,
		UNUSED_AUTHENTICATOR,
		UNUSED_TOKEN_ISSUER,
		UNUSED_TEAM_QUERY,
		new UserValidator(roleRepository)
	);

	AuthServiceTest() {
		roleRepository.given(ADMIN_ROLE_ID, "ADMIN");
		roleRepository.given(USER_ROLE_ID, "USER");
		roleRepository.given(PLATFORM_ADMIN_ROLE_ID, Role.PLATFORM_ADMIN);
	}

	private static CreateUserCommand createCommand(Long tenantId, Long roleId, String username) {
		return new CreateUserCommand(
			tenantId, roleId, username, "raw-password", "이름", "부서", "a@b.c", "010-0000-0000");
	}

	private static UpdateUserCommand updateCommand(Long userId, Long tenantId, Long roleId) {
		return new UpdateUserCommand(userId, tenantId, roleId, null, null, null, null);
	}

	@Nested
	@DisplayName("회원 생성")
	class CreateUser {

		@Test
		@DisplayName("PLATFORM_ADMIN 역할로는 회원을 만들 수 없다")
		void 운영자_역할로는_생성할_수_없다() {
			assertThatThrownBy(() -> authService.createUser(
				createCommand(TENANT, PLATFORM_ADMIN_ROLE_ID, "attacker")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_ASSIGNABLE);

			assertThat(userRepository.count()).isZero();
		}

		@Test
		@DisplayName("테넌트 역할이면 생성되고 비밀번호는 암호화되어 저장된다")
		void 테넌트_역할은_생성된다() {
			authService.createUser(createCommand(TENANT, ADMIN_ROLE_ID, "member"));

			User saved = userRepository.findByUsername("member").orElseThrow();
			assertThat(saved.getTenantId()).isEqualTo(TENANT);
			assertThat(saved.getRoleId()).isEqualTo(ADMIN_ROLE_ID);
			assertThat(saved.getPassword()).isEqualTo("encoded:raw-password");
		}

		@Test
		@DisplayName("아이디가 이미 있으면 USER_USERNAME_DUPLICATED — 아이디는 테넌트와 무관하게 전역 유일하다")
		void 중복_아이디는_충돌이다() {
			authService.createUser(createCommand(TENANT, USER_ROLE_ID, "duplicated"));

			assertThatThrownBy(() -> authService.createUser(
				createCommand(OTHER_TENANT, USER_ROLE_ID, "duplicated")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_USERNAME_DUPLICATED);
		}

		@Test
		@DisplayName("중복 아이디는 409다 — 정상적인 비즈니스 거부이므로 500(서버 오류)이 아니다")
		void 중복_아이디는_409다() {
			authService.createUser(createCommand(TENANT, USER_ROLE_ID, "duplicated"));

			assertThatThrownBy(() -> authService.createUser(
				createCommand(TENANT, USER_ROLE_ID, "duplicated")))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode().getStatus().value())
				.isEqualTo(409);
		}

		@Test
		@DisplayName("존재하지 않는 역할이면 ROLE_NOT_FOUND")
		void 없는_역할이면_찾을_수_없다() {
			assertThatThrownBy(() -> authService.createUser(createCommand(TENANT, 404L, "member")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("회원 수정 — 생성 경로만 막으면 여기로 우회된다")
	class UpdateUser {

		@Test
		@DisplayName("역할을 PLATFORM_ADMIN으로 바꿀 수 없다")
		void 운영자_역할로_변경할_수_없다() {
			User member = userRepository.given(TENANT, USER_ROLE_ID, "member");

			assertThatThrownBy(() -> authService.updateUser(
				updateCommand(member.getId(), TENANT, PLATFORM_ADMIN_ROLE_ID)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_ASSIGNABLE);

			assertThat(userRepository.peek(member.getId()).orElseThrow().getRoleId())
				.isEqualTo(USER_ROLE_ID);
		}

		@Test
		@DisplayName("자기 자신의 역할을 올리는 경로도 같은 규칙으로 막힌다")
		void 자기_계정_권한_상승도_막힌다() {
			User admin = userRepository.given(TENANT, ADMIN_ROLE_ID, "tenant-admin");

			assertThatThrownBy(() -> authService.updateUser(
				updateCommand(admin.getId(), TENANT, PLATFORM_ADMIN_ROLE_ID)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_ASSIGNABLE);

			assertThat(userRepository.peek(admin.getId()).orElseThrow().getRoleId())
				.isEqualTo(ADMIN_ROLE_ID);
		}

		@Test
		@DisplayName("테넌트 역할로는 변경된다")
		void 테넌트_역할로는_변경된다() {
			User member = userRepository.given(TENANT, USER_ROLE_ID, "member");

			authService.updateUser(updateCommand(member.getId(), TENANT, ADMIN_ROLE_ID));

			assertThat(userRepository.peek(member.getId()).orElseThrow().getRoleId())
				.isEqualTo(ADMIN_ROLE_ID);
		}

		@Test
		@DisplayName("roleId가 null이면 역할을 그대로 두고 나머지만 수정한다")
		void roleId가_null이면_역할은_유지된다() {
			User member = userRepository.given(TENANT, USER_ROLE_ID, "member");

			authService.updateUser(new UpdateUserCommand(
				member.getId(), TENANT, null, "새이름", null, null, null));

			User updated = userRepository.peek(member.getId()).orElseThrow();
			assertThat(updated.getRoleId()).isEqualTo(USER_ROLE_ID);
			assertThat(updated.getName()).isEqualTo("새이름");
		}
	}

	@Nested
	@DisplayName("교차 테넌트 차단")
	class TenantIsolation {

		@Test
		@DisplayName("다른 테넌트 사용자는 수정할 수 없고 404로 존재를 숨긴다")
		void 남의_테넌트_사용자는_수정할_수_없다() {
			User stranger = userRepository.given(OTHER_TENANT, USER_ROLE_ID, "stranger");

			assertThatThrownBy(() -> authService.updateUser(
				updateCommand(stranger.getId(), TENANT, ADMIN_ROLE_ID)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

			assertThat(userRepository.peek(stranger.getId()).orElseThrow().getRoleId())
				.isEqualTo(USER_ROLE_ID);
		}

		@Test
		@DisplayName("다른 테넌트 사용자는 삭제할 수 없다")
		void 남의_테넌트_사용자는_삭제할_수_없다() {
			User stranger = userRepository.given(OTHER_TENANT, USER_ROLE_ID, "stranger");

			assertThatThrownBy(() -> authService.deleteUser(stranger.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

			assertThat(userRepository.peek(stranger.getId())).isPresent();
		}

		@Test
		@DisplayName("자기 테넌트 사용자는 삭제된다")
		void 자기_테넌트_사용자는_삭제된다() {
			User member = userRepository.given(TENANT, USER_ROLE_ID, "member");

			authService.deleteUser(member.getId(), TENANT);

			assertThat(userRepository.peek(member.getId())).isEmpty();
		}
	}

	@Nested
	@DisplayName("부트스트랩 예외 경로")
	class Bootstrap {

		@Test
		@DisplayName("createPlatformAdmin은 역할 부여 제한을 적용하지 않는다 — 의도된 유일한 예외")
		void 부트스트랩만_운영자를_만들_수_있다() {
			assertThatCode(() -> authService.createPlatformAdmin(
				createCommand(TENANT, PLATFORM_ADMIN_ROLE_ID, "platform-admin")))
				.doesNotThrowAnyException();

			assertThat(userRepository.findByUsername("platform-admin").orElseThrow().getRoleId())
				.isEqualTo(PLATFORM_ADMIN_ROLE_ID);
		}

		@Test
		@DisplayName("같은 역할이라도 일반 생성 경로로는 막힌다 — 두 경로의 차이가 곧 보안 경계다")
		void 일반_경로와_부트스트랩_경로는_다르다() {
			assertThatThrownBy(() -> authService.createUser(
				createCommand(TENANT, PLATFORM_ADMIN_ROLE_ID, "attacker")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_ASSIGNABLE);
		}
	}

	// --- 이 유스케이스들이 쓰지 않는 협력자: 호출되면 즉시 실패시켜 "여기까지 오지 않는다"를 구조로 고정한다 ---

	private static final PasswordEncryptor FAKE_ENCRYPTOR = new PasswordEncryptor() {
		@Override
		public String encode(String rawPassword) {
			return "encoded:" + rawPassword;
		}

		@Override
		public boolean matches(String rawPassword, String encodedPassword) {
			throw new UnsupportedOperationException();
		}
	};

	private static final Authenticator UNUSED_AUTHENTICATOR = new Authenticator() {
		@Override
		public AuthenticatedUser authenticate(String username, String password) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<AuthenticatedUser> loadAuthenticatedUser(String username) {
			throw new UnsupportedOperationException();
		}
	};

	private static final TokenIssuer UNUSED_TOKEN_ISSUER = new TokenIssuer() {
		@Override
		public TokenResult issue(AuthenticatedUser user) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String issueAccessToken(AuthenticatedUser user) {
			throw new UnsupportedOperationException();
		}
	};

	private static final TeamQueryUseCase UNUSED_TEAM_QUERY = new TeamQueryUseCase() {
		@Override
		public TeamSummary getTeamSummary(Long teamId, Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public UserTeamSummary getUserTeamSummary(Long userId, Long tenantId) {
			throw new UnsupportedOperationException();
		}
	};
}
