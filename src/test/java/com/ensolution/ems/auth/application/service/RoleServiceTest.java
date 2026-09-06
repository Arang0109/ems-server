package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.FakeRoleRepository;
import com.ensolution.ems.auth.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 역할 목록이 <b>부여 가능한 것만</b> 담는다는 계약을 고정한다.
 *
 * <p>이 목록은 관리자 페이지의 역할 선택지가 된다. {@code PLATFORM_ADMIN}이 섞여 들어가면
 * 사용자는 고를 수 있지만 저장 시점에 {@code ROLE_NOT_ASSIGNABLE}(403)을 받는다 —
 * <b>서버가 고를 수 없는 것을 선택지로 준</b> 셈이다.
 *
 * <p>필터를 클라이언트에 두지 않은 이유도 여기서 함께 고정된다. 판정 근거는
 * {@link Role#isPlatformAdmin()} 하나이며, 목록 생성과 부여 검증({@code UserValidator})이
 * 같은 것을 본다. 클라이언트마다 같은 규칙을 다시 구현하면 그 일치가 깨진다.
 */
class RoleServiceTest {

	private final FakeRoleRepository roleRepository = new FakeRoleRepository();
	private final RoleService roleService = new RoleService(roleRepository);

	@Test
	@DisplayName("PLATFORM_ADMIN은 목록에서 빠진다 — 부여할 수 없는 역할은 선택지가 되지 않는다")
	void 운영자_역할은_목록에_없다() {
		roleRepository.given(1L, "ADMIN");
		roleRepository.given(4L, Role.PLATFORM_ADMIN);
		roleRepository.given(5L, "USER");

		assertThat(roleService.getAssignableRoles())
			.extracting(Role::getName)
			.containsExactly("ADMIN", "USER");
	}

	@Test
	@DisplayName("나머지 테넌트 역할은 그대로 내려간다")
	void 테넌트_역할은_모두_내려간다() {
		roleRepository.given(1L, "ADMIN");
		roleRepository.given(2L, "LAB");
		roleRepository.given(3L, "FIELD");
		roleRepository.given(5L, "USER");
		roleRepository.given(6L, "DOC");

		assertThat(roleService.getAssignableRoles()).hasSize(5);
	}

	@Test
	@DisplayName("부여 가능한 역할이 하나도 없으면 빈 목록이다 — 예외를 던지지 않는다")
	void 부여할_역할이_없으면_빈_목록이다() {
		roleRepository.given(4L, Role.PLATFORM_ADMIN);

		assertThat(roleService.getAssignableRoles()).isEmpty();
	}
}
