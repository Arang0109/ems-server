package com.ensolution.ems.admin.presentation.controller;

import com.ensolution.ems.admin.presentation.mapper.MemberMapperImpl;
import com.ensolution.ems.admin.presentation.request.CreateMemberRequest;
import com.ensolution.ems.admin.presentation.request.UpdateMemberRequest;
import com.ensolution.ems.admin.presentation.response.MemberResponse;
import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.UpdateUserCommand;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원 관리 경로가 <b>principal의 tenantId를 빠짐없이 원장 모듈로 실어 보내는지</b> 고정한다.
 *
 * <p>이 모듈에는 자체 원장이 없어 WHERE 절이 없다. 격리는 전적으로 "컨트롤러가 tenantId를 Command에
 * 넣는가"에 달려 있고, 넣지 않아도 <b>컴파일은 통과한다</b> — 그 자리에서 교차 테넌트가 된다.
 * 단건 경로 3개(GET·PUT·DELETE)는 실제로 이 파라미터가 없어 다른 테넌트의 계정을 다룰 수 있었던 이력이 있다.
 *
 * <p>Member 중간 계층(도메인·커맨드·서비스·PortMapper)을 걷어내고 auth의 {@code port/in}을 직접 쓰도록
 * 바꾼 뒤에도 같은 값이 같은 자리로 간다는 것을 함께 확인한다 — 리팩터링이 격리를 건드리지 않았다는 근거다.
 */
class MemberControllerTest {

	private static final long TENANT = 1L;
	private static final long ACTOR_USER_ID = 100L;

	private final RecordingUserCommand userCommand = new RecordingUserCommand();
	private final StubUserQuery userQuery = new StubUserQuery();

	private final MemberController controller =
		new MemberController(userCommand, userQuery, new MemberMapperImpl());

	/** 테넌트 1에 속한 ADMIN이 로그인한 상태. */
	private static CustomUserDetails principal() {
		return new CustomUserDetails(
			ACTOR_USER_ID, TENANT, "테넌트", "admin", "encoded", "관리자",
			List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
	}

	@Nested
	@DisplayName("tenantId 전달")
	class TenantPropagation {

		@Test
		@DisplayName("등록: principal의 tenantId가 CreateUserCommand에 실린다")
		void 등록에_tenantId가_실린다() {
			controller.createMember(
				new CreateMemberRequest(2L, "newbie", "pw", "신입", "분석팀", "a@b.c", "010-1111-2222"),
				principal());

			CreateUserCommand sent = userCommand.created.get(0);
			assertThat(sent.tenantId()).isEqualTo(TENANT);
			assertThat(sent.username()).isEqualTo("newbie");
			assertThat(sent.password()).isEqualTo("pw");
			assertThat(sent.roleId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("수정: path의 id와 principal의 tenantId가 함께 실린다")
		void 수정에_id와_tenantId가_실린다() {
			controller.updateMember(
				55L,
				new UpdateMemberRequest(3L, "바뀐이름", null, null, null),
				principal());

			UpdateUserCommand sent = userCommand.updated.get(0);
			assertThat(sent.userId()).isEqualTo(55L);
			assertThat(sent.tenantId()).isEqualTo(TENANT);
			assertThat(sent.roleId()).isEqualTo(3L);
			assertThat(sent.name()).isEqualTo("바뀐이름");
		}

		@Test
		@DisplayName("삭제: id와 tenantId가 그대로 전달된다")
		void 삭제에_tenantId가_실린다() {
			controller.deleteMember(55L, principal());

			assertThat(userCommand.deleted).containsExactly(List.of(55L, TENANT));
		}

		@Test
		@DisplayName("단건 조회: tenantId 없이 조회하지 않는다")
		void 단건_조회에_tenantId가_실린다() {
			controller.getMember(55L, principal());

			assertThat(userQuery.getUserCalls).containsExactly(List.of(55L, TENANT));
		}

		@Test
		@DisplayName("목록 조회: 자기 테넌트 범위로만 조회한다")
		void 목록_조회에_tenantId가_실린다() {
			controller.getMemberList(principal());

			assertThat(userQuery.getListCalls).containsExactly(TENANT);
		}
	}

	@Nested
	@DisplayName("응답 변환 — UserSummary를 중간 도메인 없이 곧장 옮긴다")
	class ResponseMapping {

		@Test
		@DisplayName("userId가 응답의 id가 되고 나머지 필드는 그대로 옮겨진다")
		void 요약이_응답으로_옮겨진다() {
			userQuery.given(new UserSummary(
				7L, TENANT, "member", "홍길동", 2L, "USER", "분석팀", "a@b.c", "010-3333-4444"));

			ApiResponse<MemberResponse> body = controller.getMember(7L, principal()).getBody();
			MemberResponse response = body.data();

			assertThat(response.id()).isEqualTo(7L);
			assertThat(response.tenantId()).isEqualTo(TENANT);
			assertThat(response.username()).isEqualTo("member");
			assertThat(response.name()).isEqualTo("홍길동");
			assertThat(response.roleId()).isEqualTo(2L);
			assertThat(response.role()).isEqualTo("USER");
			assertThat(response.department()).isEqualTo("분석팀");
			assertThat(response.email()).isEqualTo("a@b.c");
			assertThat(response.tel()).isEqualTo("010-3333-4444");
		}

		@Test
		@DisplayName("목록도 같은 규칙으로 변환된다")
		void 목록도_같은_규칙이다() {
			userQuery.given(new UserSummary(
				7L, TENANT, "member", "홍길동", 2L, "USER", null, null, null));

			ApiResponse<List<MemberResponse>> body = controller.getMemberList(principal()).getBody();

			assertThat(body.data())
				.extracting(MemberResponse::id, MemberResponse::username)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(7L, "member"));
		}
	}

	/** 컨트롤러가 무엇을 넘겼는지 기록하는 {@link UserCommandUseCase}. */
	private static class RecordingUserCommand implements UserCommandUseCase {

		private final List<CreateUserCommand> created = new ArrayList<>();
		private final List<UpdateUserCommand> updated = new ArrayList<>();
		private final List<List<Long>> deleted = new ArrayList<>();

		@Override
		public void createUser(CreateUserCommand command) {
			created.add(command);
		}

		@Override
		public void updateUser(UpdateUserCommand command) {
			updated.add(command);
		}

		@Override
		public void deleteUser(Long userId, Long tenantId) {
			deleted.add(List.of(userId, tenantId));
		}

		/** 부트스트랩 전용 경로다. 관리 화면에서 닿으면 안 되므로 호출되는 순간 실패시킨다. */
		@Override
		public void createPlatformAdmin(CreateUserCommand command) {
			throw new UnsupportedOperationException("관리 화면은 이 경로를 쓰지 않는다");
		}
	}

	/** 조회 호출을 기록하고 준비된 요약을 돌려주는 {@link UserQueryUseCase}. */
	private static class StubUserQuery implements UserQueryUseCase {

		private final List<List<Long>> getUserCalls = new ArrayList<>();
		private final List<Long> getListCalls = new ArrayList<>();
		private UserSummary summary =
			new UserSummary(1L, TENANT, "default", "기본", 2L, "USER", null, null, null);

		void given(UserSummary summary) {
			this.summary = summary;
		}

		@Override
		public UserSummary getUser(Long userId, Long tenantId) {
			getUserCalls.add(List.of(userId, tenantId));
			return summary;
		}

		@Override
		public List<UserSummary> getUserList(Long tenantId) {
			getListCalls.add(tenantId);
			return List.of(summary);
		}

		@Override
		public boolean existsByUsername(String username) {
			throw new UnsupportedOperationException();
		}
	}
}
