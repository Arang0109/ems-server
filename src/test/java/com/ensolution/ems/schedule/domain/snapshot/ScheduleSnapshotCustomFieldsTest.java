package com.ensolution.ems.schedule.domain.snapshot;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code withCustomFields}의 전체 채택 시맨틱을 고정한다 — 맵을 통째로 교체하고, 빈 값은 항목 삭제로 읽으며,
 * 다른 노드는 건드리지 않는다. 반환된 맵은 불변이라 소비처가 문서를 몰래 고칠 수 없다.
 */
class ScheduleSnapshotCustomFieldsTest {

	private static ScheduleSnapshot snapshotWith(Map<String, String> customFields) {
		TeamSnapshot team = new TeamSnapshot(20L, "1팀", "홍길동", "김철수", List.of());
		return new ScheduleSnapshot("1", 1L, 1L, 3L, null, null, team, null, List.of(), customFields);
	}

	@Test
	void 맵을_통째로_교체한다() {
		ScheduleSnapshot original = snapshotWith(Map.of("siteCode", "A-01", "note", "야간"));

		ScheduleSnapshot changed = original.withCustomFields(Map.of("inspector", "홍길동"));

		assertThat(changed.customFields()).containsExactly(Map.entry("inspector", "홍길동"));
	}

	@Test
	void null_이면_빈_맵이_된다() {
		ScheduleSnapshot changed = snapshotWith(Map.of("siteCode", "A-01")).withCustomFields(null);

		assertThat(changed.customFields()).isEmpty();
	}

	@Test
	void 빈_값과_null_값_항목은_지운다() {
		Map<String, String> values = new HashMap<>();
		values.put("siteCode", "A-01");
		values.put("blank", "   ");
		values.put("none", null);

		ScheduleSnapshot changed = snapshotWith(null).withCustomFields(values);

		assertThat(changed.customFields()).containsExactly(Map.entry("siteCode", "A-01"));
	}

	@Test
	void 다른_노드와_버전은_그대로다() {
		ScheduleSnapshot original = snapshotWith(null);

		ScheduleSnapshot changed = original.withCustomFields(Map.of("siteCode", "A-01"));

		assertThat(changed.version()).isEqualTo(3L);
		assertThat(changed.team()).isSameAs(original.team());
		assertThat(changed.items()).isSameAs(original.items());
	}

	@Test
	void 반환된_맵은_불변이다() {
		ScheduleSnapshot changed = snapshotWith(null).withCustomFields(Map.of("siteCode", "A-01"));

		assertThatThrownBy(() -> changed.customFields().put("x", "y"))
			.isInstanceOf(UnsupportedOperationException.class);
	}
}
