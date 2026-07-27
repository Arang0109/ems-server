package com.ensolution.ems.schedule.domain.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 측정 시점 팀 스냅샷의 부분 교체 규칙 검증. 원장 연결키 보존이 핵심이다. */
class TeamSnapshotTest {

	private static TeamSnapshot existing() {
		return new TeamSnapshot(1L, "1팀", 10L, "홍길동", 20L, "김철수",
			"E1", "E2", "E3", "E4");
	}

	@Nested
	@DisplayName("withMembers")
	class WithMembers {

		@Test
		void 측정자_표기명을_교체한다() {
			TeamSnapshot changed = existing().withMembers("이측정", "박보조");

			assertThat(changed.mentorName()).isEqualTo("이측정");
			assertThat(changed.menteeName()).isEqualTo("박보조");
		}

		@Test
		void 원장_연결키와_장비_슬롯은_보존된다() {
			TeamSnapshot changed = existing().withMembers("이측정", "박보조");

			assertThat(changed.teamId()).isEqualTo(1L);
			assertThat(changed.teamName()).isEqualTo("1팀");
			assertThat(changed.mentorUserId()).isEqualTo(10L);
			assertThat(changed.menteeUserId()).isEqualTo(20L);
			assertThat(changed.particleSamplerId()).isEqualTo("E1");
			assertThat(changed.gasSamplerId()).isEqualTo("E2");
			assertThat(changed.pitotTubeId()).isEqualTo("E3");
			assertThat(changed.nozzleId()).isEqualTo("E4");
		}

		@Test
		void 전달되지_않거나_공백인_이름은_기존_값을_유지한다() {
			assertThat(existing().withMembers(null, null)).isEqualTo(existing());
			assertThat(existing().withMembers("  ", "").mentorName()).isEqualTo("홍길동");
			assertThat(existing().withMembers("  ", "").menteeName()).isEqualTo("김철수");
		}

		@Test
		void 한_명만_교체할_수_있다() {
			TeamSnapshot changed = existing().withMembers(null, "박보조");

			assertThat(changed.mentorName()).isEqualTo("홍길동");
			assertThat(changed.menteeName()).isEqualTo("박보조");
		}
	}
}
