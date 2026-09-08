package com.ensolution.ems.auth.presentation.mapper;

import com.ensolution.ems.auth.application.FakeRoleRepository;
import com.ensolution.ems.auth.application.FakeUserRepository;
import com.ensolution.ems.auth.application.service.UserService;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.presentation.response.UserListResponse;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code GET /api/users}가 내보내는 것을 고정한다.
 *
 * <p>이 경로는 ADMIN 전용인 {@code /api/admin/members}와 달리 <b>전체 인증 사용자</b>에게 열려 있다.
 * 그래서 "무엇이 나가지 않는가"가 곧 이 엔드포인트의 존재 이유다 — 응답에 로그인 아이디나 연락처가
 * 다시 끼어들면 권한을 낮춘 것과 같아지므로, 필드 목록 자체를 테스트로 못 박는다.
 *
 * <p>테넌트 격리도 함께 고정한다. 이 조회에는 자체 WHERE 절이 없고
 * {@code principal.getTenantId()} 전달 하나에 의존하므로 회귀가 조용히 지나간다.
 */
class UserListResponseMappingTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	private static final Long FIELD_ROLE = 10L;
	private static final Long LAB_ROLE = 11L;

	private FakeUserRepository userRepository;
	private UserService userService;

	private final UserMapper mapper = new UserMapperImpl();

	@BeforeEach
	void setUp() {
		userRepository = new FakeUserRepository();

		FakeRoleRepository roleRepository = new FakeRoleRepository();
		roleRepository.given(FIELD_ROLE, "FIELD");
		roleRepository.given(LAB_ROLE, "LAB");

		userService = new UserService(userRepository, roleRepository);
	}

	private void givenUser(Long tenantId, Long roleId, String username, String name, String department) {
		userRepository.save(User.builder()
			.tenantId(tenantId)
			.roleId(roleId)
			.username(username)
			.password("encoded:" + username)
			.name(name)
			.department(department)
			.email(username + "@ensolution.co.kr")
			.tel("010-0000-0000")
			.build());
	}

	private List<UserListResponse> listFor(Long tenantId) {
		return mapper.toListResponses(userService.getUserList(tenantId));
	}

	private static List<String> responseComponents() {
		return Arrays.stream(UserListResponse.class.getRecordComponents())
			.map(RecordComponent::getName)
			.toList();
	}

	@Nested
	@DisplayName("응답 필드 범위")
	class ResponseFields {

		@Test
		void 선택지에_필요한_네_필드만_갖는다() {
			assertThat(responseComponents()).containsExactly("userId", "name", "department", "role");
		}

		@Test
		void 로그인_아이디와_연락처는_노출되지_않는다() {
			assertThat(responseComponents())
				.doesNotContain("username", "password", "email", "tel", "roleId", "tenantId");
		}

		@Test
		void 역할_이름이_채워진다() {
			givenUser(TENANT, FIELD_ROLE, "field1", "김측정", "측정1팀");

			assertThat(listFor(TENANT))
				.extracting(UserListResponse::name, UserListResponse::department, UserListResponse::role)
				.containsExactly(Tuple.tuple("김측정", "측정1팀", "FIELD"));
		}
	}

	@Nested
	@DisplayName("테넌트 격리")
	class TenantIsolation {

		@Test
		void 자기_테넌트_사용자만_반환한다() {
			givenUser(TENANT, FIELD_ROLE, "field1", "김측정", "측정1팀");
			givenUser(TENANT, LAB_ROLE, "lab1", "이분석", "실험실");
			givenUser(OTHER_TENANT, FIELD_ROLE, "other1", "타사직원", "측정팀");

			assertThat(listFor(TENANT))
				.extracting(UserListResponse::name)
				.containsExactly("김측정", "이분석");
		}

		@Test
		void 다른_테넌트로_조회하면_그_테넌트의_사용자만_나온다() {
			givenUser(TENANT, FIELD_ROLE, "field1", "김측정", "측정1팀");
			givenUser(OTHER_TENANT, FIELD_ROLE, "other1", "타사직원", "측정팀");

			assertThat(listFor(OTHER_TENANT))
				.extracting(UserListResponse::name)
				.containsExactly("타사직원");
		}
	}
}
