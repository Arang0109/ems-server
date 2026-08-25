package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정항목 순서 변경 요청이 계획의 항목 집합과 정확히 일치해야 한다는 규칙 검증.
 * 순서가 성적서의 항목 배치를 결정하므로, 집합이 어긋난 요청은 부분 저장 없이 통째로 거절해야 한다.
 */
class ScheduleValidatorTest {

	private ScheduleValidator validator;

	@BeforeEach
	void setUp() {
		// 순서 검증은 포트를 조회하지 않는다(서비스가 이미 읽은 스냅샷과 대조한다).
		validator = new ScheduleValidator(new FakeScheduleRepository());
	}

	private SamplingItemSnapshot item(Long pollutantId) {
		return new SamplingItemSnapshot(pollutantId * 10, pollutantId, null, "물질" + pollutantId, null,
			null, null, null, null, null, null, null, false);
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
}
