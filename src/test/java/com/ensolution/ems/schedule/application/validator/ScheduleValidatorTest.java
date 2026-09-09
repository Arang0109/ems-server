package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정계획 등록·편집의 두 규칙을 고정한다.
 *
 * <p><b>측정항목 순서</b>는 성적서의 항목 배치를 결정하므로, 집합이 어긋난 요청은 부분 저장 없이 통째로 거절한다.
 *
 * <p><b>측정자(사수·부사수)</b>는 팀 원장이 아니라 테넌트 사용자 전체에서 고르는 값이라 팀 소속으로는
 * 검증되지 않는다. 그래서 여기서 tenant 소속을 직접 확인하며, 미존재와 타 tenant를 같은 코드로 은닉한다.
 */
class ScheduleValidatorTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	/**
	 * auth 사용자 조회 스텁. 실제 어댑터와 동일하게 <b>tenant 필터링을 재현</b>하고
	 * 미존재·타 tenant를 모두 {@code USER_NOT_FOUND}로 돌려준다 — 그래야 Validator가 그것을
	 * 사수·부사수 문맥으로 바꿔 던지는지 검증할 수 있다.
	 */
	private static class StubUserQuery implements UserQueryUseCase {

		private final Map<Long, UserSummary> users = new HashMap<>();

		void given(Long userId, Long tenantId, String name) {
			users.put(userId, new UserSummary(userId, tenantId, "user" + userId, name, 10L, "FIELD", null, null, null));
		}

		@Override
		public UserSummary getUser(Long userId, Long tenantId) {
			UserSummary found = users.get(userId);
			if (found == null || !found.tenantId().equals(tenantId)) {
				throw new CustomException(ErrorCode.USER_NOT_FOUND);
			}
			return found;
		}

		@Override
		public List<UserSummary> getUserList(Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean existsByUsername(String username) {
			throw new UnsupportedOperationException();
		}
	}

	private StubUserQuery userQuery;
	private ScheduleValidator validator;

	@BeforeEach
	void setUp() {
		// 순서 검증은 포트를 조회하지 않는다(서비스가 이미 읽은 스냅샷과 대조한다).
		userQuery = new StubUserQuery();
		validator = new ScheduleValidator(new FakeScheduleRepository(), userQuery);
	}

	private SamplingItemSnapshot item(Long pollutantId) {
		return new SamplingItemSnapshot(pollutantId * 10, pollutantId, null, "물질" + pollutantId, null,
			null, null, null, null, null, null, null, false, null);
	}

	private List<SamplingItemSnapshot> items(Long... pollutantIds) {
		return Arrays.stream(pollutantIds).map(this::item).toList();
	}

	@Test
	void 같은_집합을_다른_순서로_보내면_통과한다() {
		assertThatCode(() -> validator.requireExactItemOrder(items(1L, 2L, 3L), List.of(3L, 1L, 2L)))
			.doesNotThrowAnyException();
	}

	@Test
	void 순서가_그대로여도_통과한다() {
		assertThatCode(() -> validator.requireExactItemOrder(items(1L, 2L), List.of(1L, 2L)))
			.doesNotThrowAnyException();
	}

	@Test
	void 중복된_id가_있으면_거절한다() {
		assertThatThrownBy(() -> validator.requireExactItemOrder(items(1L, 2L), List.of(1L, 1L, 2L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
	}

	@Test
	void 항목이_빠지면_거절한다() {
		// 부분 목록은 나머지 항목이 어디에 놓이는지 정의하지 못한다
		assertThatThrownBy(() -> validator.requireExactItemOrder(items(1L, 2L, 3L), List.of(1L, 2L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
	}

	@Test
	void 계획에_없는_물질이_섞이면_거절한다() {
		assertThatThrownBy(() -> validator.requireExactItemOrder(items(1L, 2L), List.of(1L, 2L, 99L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
	}

	@Test
	void 화면을_연_뒤_항목이_교체되었으면_거절한다() {
		// 내가 보던 목록은 1,2,3 이지만 그 사이 서버 항목이 1,2,4 로 바뀐 상황
		assertThatThrownBy(() -> validator.requireExactItemOrder(items(1L, 2L, 4L), List.of(3L, 1L, 2L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
	}

	@Test
	void 항목이_없는_계획은_어떤_요청도_거절한다() {
		assertThatThrownBy(() -> validator.requireExactItemOrder(null, List.of(1L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
	}

	@Nested
	@DisplayName("requireMeasurersInTenant")
	class RequireMeasurersInTenant {

		@Test
		void 두_측정자가_모두_같은_테넌트면_통과한다() {
			userQuery.given(10L, TENANT, "김사수");
			userQuery.given(20L, TENANT, "이부사수");

			assertThatCode(() -> validator.requireMeasurersInTenant(10L, 20L, TENANT))
				.doesNotThrowAnyException();
		}

		@Test
		void 미배정은_통과한다() {
			// 사수·부사수를 보내지 않던 기존 클라이언트가 그대로 동작해야 한다.
			assertThatCode(() -> validator.requireMeasurersInTenant(null, null, TENANT))
				.doesNotThrowAnyException();
		}

		@Test
		void 한쪽만_배정해도_통과한다() {
			userQuery.given(10L, TENANT, "김사수");

			assertThatCode(() -> validator.requireMeasurersInTenant(10L, null, TENANT))
				.doesNotThrowAnyException();
		}

		@Test
		void 존재하지_않는_사수는_거절한다() {
			assertThatThrownBy(() -> validator.requireMeasurersInTenant(99L, null, TENANT))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.SCHEDULE_MENTOR_NOT_FOUND);
		}

		@Test
		void 존재하지_않는_부사수는_부사수_코드로_거절한다() {
			userQuery.given(10L, TENANT, "김사수");

			assertThatThrownBy(() -> validator.requireMeasurersInTenant(10L, 99L, TENANT))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.SCHEDULE_MENTEE_NOT_FOUND);
		}

		@Test
		void 다른_테넌트의_사용자는_미존재와_같게_거절한다() {
			// 교차 테넌트 배정. 리소스 존재를 드러내지 않으려 미존재와 같은 코드로 은닉한다.
			userQuery.given(10L, OTHER_TENANT, "타사직원");

			assertThatThrownBy(() -> validator.requireMeasurersInTenant(10L, null, TENANT))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.SCHEDULE_MENTOR_NOT_FOUND);
		}

		@Test
		void 같은_사람을_사수와_부사수로_지정하면_거절한다() {
			userQuery.given(10L, TENANT, "김사수");

			assertThatThrownBy(() -> validator.requireMeasurersInTenant(10L, 10L, TENANT))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.SCHEDULE_MEASURER_DUPLICATED);
		}

		@Test
		void 둘_다_미배정이면_동일인_규칙에_걸리지_않는다() {
			// null == null 을 중복으로 판정하면 미배정 계획을 등록할 수 없게 된다.
			assertThatCode(() -> validator.requireMeasurersInTenant(null, null, TENANT))
				.doesNotThrowAnyException();
		}
	}
}
