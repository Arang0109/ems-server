package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.equipment.domain.EquipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 측정 시점 팀 스냅샷의 부분 교체 규칙 검증. 원장 연결키 보존이 핵심이다. */
class TeamSnapshotTest {

	private static EquipmentSnapshot equipment(String id, EquipType type) {
		return new EquipmentSnapshot(id, type, null, null, null, null, null, null, null, null);
	}

	private static TeamSnapshot patch(String mentorName, String menteeName) {
		return new TeamSnapshot(null, null, mentorName, menteeName, null);
	}

	private static TeamSnapshot existing() {
		return new TeamSnapshot(1L, "1팀", "홍길동", "김철수",
			List.of(equipment("E1", EquipType.PITOT_TUBE), equipment("E2", EquipType.NOZZLE)));
	}

	@Nested
	@DisplayName("merge")
	class Merge {

		@Test
		void 측정자_표기명을_교체한다() {
			TeamSnapshot changed = existing().merge(patch("이측정", "박보조"));

			assertThat(changed.mentorName()).isEqualTo("이측정");
			assertThat(changed.menteeName()).isEqualTo("박보조");
		}

		@Test
		void 원장_연결키와_장비_목록은_보존된다() {
			TeamSnapshot changed = existing().merge(patch("이측정", "박보조"));

			assertThat(changed.teamId()).isEqualTo(1L);
			assertThat(changed.teamName()).isEqualTo("1팀");
			assertThat(changed.equipments()).extracting(EquipmentSnapshot::equipmentId)
				.containsExactly("E1", "E2");
		}

		@Test
		void 전달되지_않거나_공백인_이름은_기존_값을_유지한다() {
			assertThat(existing().merge(patch(null, null))).isEqualTo(existing());
			assertThat(existing().merge(patch("  ", "")).mentorName()).isEqualTo("홍길동");
			assertThat(existing().merge(patch("  ", "")).menteeName()).isEqualTo("김철수");
		}

		@Test
		void 한_명만_교체할_수_있다() {
			TeamSnapshot changed = existing().merge(patch(null, "박보조"));

			assertThat(changed.mentorName()).isEqualTo("홍길동");
			assertThat(changed.menteeName()).isEqualTo("박보조");
		}
	}

	@Nested
	@DisplayName("withEquipments")
	class WithEquipments {

		@Test
		void 장비_목록을_통째로_교체한다() {
			TeamSnapshot changed = existing()
				.withEquipments(List.of(equipment("E9", EquipType.GAS_SAMPLER)));

			assertThat(changed.equipments()).extracting(EquipmentSnapshot::equipmentId)
				.containsExactly("E9");
		}

		@Test
		void 빈_목록은_장비_없음을_뜻한다() {
			assertThat(existing().withEquipments(List.of()).equipments()).isEmpty();
		}

		@Test
		void 측정자와_원장_연결키는_보존된다() {
			TeamSnapshot changed = existing().withEquipments(List.of());

			assertThat(changed.teamId()).isEqualTo(1L);
			assertThat(changed.mentorName()).isEqualTo("홍길동");
			assertThat(changed.menteeName()).isEqualTo("김철수");
		}
	}
}
