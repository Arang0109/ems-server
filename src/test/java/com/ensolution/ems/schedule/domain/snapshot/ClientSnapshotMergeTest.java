package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 의뢰기관 스냅샷 트리(의뢰기관→사업장→측정시설)의 부분 병합 규칙 검증.
 * PATCH /api/schedules/{id}/client의 동작 계약이며, 원장 연결키 보존과 미전달 필드 유지가 핵심이다.
 */
class ClientSnapshotMergeTest {

	private static final FacilitySnapshot BOILER =
		new FacilitySnapshot(10L, "보일러", "100", null, null, "LNG", "가스", "Nm³");
	private static final FacilitySnapshot DRYER =
		new FacilitySnapshot(11L, "건조기", "50", null, null, "B-C", "유류", "L");
	private static final PreventionSnapshot SCRUBBER = new PreventionSnapshot(20L, "흡수탑", 30.0, null, "THC", "90");

	/** 원장에서 조립된 완전한 스냅샷 트리. */
	private static ClientSnapshot existing() {
		StackSnapshot stack = new StackSnapshot(
			300L, MeasurementField.AIR, "1호 굴뚝", "SEMS-1", Grade.TYPE_2,
			"철강", 6, 25.0, 1.5, 2.0,
			Shape.RECTANGULAR, Orientation.VERTICAL,
			List.of(BOILER), List.of(SCRUBBER));
		WorkplaceSnapshot workplace = new WorkplaceSnapshot(
			200L, "1공장", "222-22-22222", "제조업", "경기도 화성시", "101동", "18000", Grade.TYPE_3, stack);
		return new ClientSnapshot(
			100L, "○○환경", "111-11-11111", "김대표", "서울시 강남구", "2층", "06000",
			"client@ex.com", "02-1234-5678", workplace);
	}

	/** 필드가 모두 비어 있는 patch. 필요한 필드만 채워서 사용한다. */
	private static ClientSnapshot emptyPatch() {
		return new ClientSnapshot(null, null, null, null, null, null, null, null, null, null);
	}

	private static ClientSnapshot patchStack(StackSnapshot stackPatch) {
		WorkplaceSnapshot workplacePatch = new WorkplaceSnapshot(
			null, null, null, null, null, null, null, null, stackPatch);
		return new ClientSnapshot(
			null, null, null, null, null, null, null, null, null, workplacePatch);
	}

	@Nested
	@DisplayName("의뢰기관 자기 필드")
	class ClientFields {

		@Test
		@DisplayName("전달한 필드만 바뀌고 나머지 트리는 그대로 유지된다")
		void mergesOnlyProvidedFields() {
			ClientSnapshot patch = new ClientSnapshot(
				null, "△△환경", null, null, null, null, "07000", null, null, null);

			ClientSnapshot merged = existing().merge(patch);

			assertThat(merged.name()).isEqualTo("△△환경");
			assertThat(merged.zipcode()).isEqualTo("07000");
			assertThat(merged.representative()).isEqualTo("김대표");
			assertThat(merged.tel()).isEqualTo("02-1234-5678");
			assertThat(merged.workplace()).isEqualTo(existing().workplace());
		}

		@Test
		@DisplayName("공백 문자열은 기존 값을 유지한다")
		void blankKeepsOriginal() {
			ClientSnapshot patch = emptyPatch();
			ClientSnapshot merged = existing().merge(
				new ClientSnapshot(null, "   ", null, null, null, null, null, null, null, null));

			assertThat(merged.name()).isEqualTo("○○환경");
			assertThat(existing().merge(patch)).isEqualTo(existing());
		}

		@Test
		@DisplayName("patch가 null이면 원본을 그대로 반환한다")
		void nullPatchReturnsSelf() {
			assertThat(existing().merge(null)).isEqualTo(existing());
		}
	}

	@Nested
	@DisplayName("원장 연결키 보존")
	class LedgerKeys {

		@Test
		@DisplayName("clientId·workplaceId·stackId는 patch 값으로 재지정되지 않는다")
		void idsAreNeverReassigned() {
			StackSnapshot stackPatch = new StackSnapshot(
				999L, null, null, null, null, null, null, null, null, null, null, null, null, null);
			WorkplaceSnapshot workplacePatch = new WorkplaceSnapshot(
				999L, null, null, null, null, null, null, null, stackPatch);
			ClientSnapshot patch = new ClientSnapshot(
				999L, null, null, null, null, null, null, null, null, workplacePatch);

			ClientSnapshot merged = existing().merge(patch);

			assertThat(merged.clientId()).isEqualTo(100L);
			assertThat(merged.workplace().workplaceId()).isEqualTo(200L);
			assertThat(merged.workplace().stack().stackId()).isEqualTo(300L);
		}
	}

	@Nested
	@DisplayName("중첩 측정시설 수정")
	class NestedStack {

		@Test
		@DisplayName("stack만 중첩 전달하면 상위 의뢰기관·사업장 필드는 보존된다")
		void stackOnlyPatchPreservesAncestors() {
			ClientSnapshot patch = patchStack(new StackSnapshot(
				null, null, null, null, null, null, 8, 30.0, null, null,
				Shape.CIRCULAR, null, null, null));

			ClientSnapshot merged = existing().merge(patch);
			StackSnapshot stack = merged.workplace().stack();

			assertThat(stack.standardOxygen()).isEqualTo(8);
			assertThat(stack.height()).isEqualTo(30.0);
			assertThat(stack.shape()).isEqualTo(Shape.CIRCULAR);
			// 미전달 필드는 유지
			assertThat(stack.name()).isEqualTo("1호 굴뚝");
			assertThat(stack.orientation()).isEqualTo(Orientation.VERTICAL);
			assertThat(stack.horizontalLength()).isEqualTo(1.5);
			// 상위 노드 보존
			assertThat(merged.name()).isEqualTo("○○환경");
			assertThat(merged.workplace().name()).isEqualTo("1공장");
			assertThat(merged.workplace().grade()).isEqualTo(Grade.TYPE_3);
		}

		/**
		 * 기준산소농도는 "미적용"이 유효한 값이라 null을 "미전달"로 해석하면 해제할 방법이 없어진다.
		 * 다른 필드와 달리 전달값을 그대로 채택한다.
		 */
		@Test
		@DisplayName("표준산소농도는 null을 전달하면 미적용으로 해제된다")
		void standardOxygenIsClearedByNull() {
			ClientSnapshot patch = patchStack(new StackSnapshot(
				null, null, null, null, null, null, null, 30.0, null, null,
				null, null, null, null));

			ClientSnapshot merged = existing().merge(patch);
			StackSnapshot stack = merged.workplace().stack();

			assertThat(stack.standardOxygen()).isNull();
			// 같은 요청의 다른 필드는 기존 규칙대로 동작한다
			assertThat(stack.height()).isEqualTo(30.0);
			assertThat(stack.name()).isEqualTo("1호 굴뚝");
		}

		@Test
		@DisplayName("측정시설을 전달하지 않으면 표준산소농도는 그대로 유지된다")
		void standardOxygenSurvivesWhenStackOmitted() {
			ClientSnapshot merged = existing().merge(new ClientSnapshot(
				null, "△△환경", null, null, null, null, null, null, null, null));

			assertThat(merged.workplace().stack().standardOxygen()).isEqualTo(6);
		}

		@Test
		@DisplayName("사업장 필드와 측정시설을 동시에 수정할 수 있다")
		void mergesWorkplaceAndStackTogether() {
			WorkplaceSnapshot workplacePatch = new WorkplaceSnapshot(
				null, "2공장", null, null, null, null, null, Grade.TYPE_1,
				new StackSnapshot(null, null, "2호 굴뚝", null, null, null, null,
					null, null, null, null, null, null, null));
			ClientSnapshot merged = existing().merge(new ClientSnapshot(
				null, null, null, null, null, null, null, null, null, workplacePatch));

			assertThat(merged.workplace().name()).isEqualTo("2공장");
			assertThat(merged.workplace().grade()).isEqualTo(Grade.TYPE_1);
			assertThat(merged.workplace().bizNumber()).isEqualTo("222-22-22222");
			assertThat(merged.workplace().stack().name()).isEqualTo("2호 굴뚝");
			assertThat(merged.workplace().stack().semsNumber()).isEqualTo("SEMS-1");
		}
	}

	@Nested
	@DisplayName("배출·방지시설 목록")
	class Lists {

		@Test
		@DisplayName("전달하면 전체 교체된다")
		void providedListReplacesEntirely() {
			ClientSnapshot merged = existing().merge(patchStack(new StackSnapshot(
				null, null, null, null, null, null, null, null, null, null, null, null,
				List.of(DRYER), null)));

			assertThat(merged.workplace().stack().facilities()).containsExactly(DRYER);
			// 미전달 목록은 유지
			assertThat(merged.workplace().stack().preventions()).containsExactly(SCRUBBER);
		}

		@Test
		@DisplayName("빈 목록을 전달하면 전부 삭제된다")
		void emptyListClearsAll() {
			ClientSnapshot merged = existing().merge(patchStack(new StackSnapshot(
				null, null, null, null, null, null, null, null, null, null, null, null,
				List.of(), null)));

			assertThat(merged.workplace().stack().facilities()).isEmpty();
		}
	}

	@Nested
	@DisplayName("하위 노드가 비어 있을 때")
	class MissingNodes {

		@Test
		@DisplayName("기존 사업장이 없으면 patch의 사업장을 그대로 채택한다")
		void adoptsWorkplaceWhenAbsent() {
			ClientSnapshot clientWithoutWorkplace = new ClientSnapshot(
				100L, "○○환경", null, null, null, null, null, null, null, null);
			WorkplaceSnapshot newWorkplace = new WorkplaceSnapshot(
				200L, "1공장", null, null, null, null, null, Grade.TYPE_3, null);

			ClientSnapshot merged = clientWithoutWorkplace.merge(new ClientSnapshot(
				null, null, null, null, null, null, null, null, null, newWorkplace));

			assertThat(merged.workplace()).isEqualTo(newWorkplace);
			assertThat(merged.clientId()).isEqualTo(100L);
		}

		@Test
		@DisplayName("기존 측정시설이 없으면 patch의 측정시설을 그대로 채택한다")
		void adoptsStackWhenAbsent() {
			WorkplaceSnapshot workplaceWithoutStack = new WorkplaceSnapshot(
				200L, "1공장", null, null, null, null, null, Grade.TYPE_3, null);
			StackSnapshot newStack = new StackSnapshot(
				300L, MeasurementField.AIR, "1호 굴뚝", null, null, null, 6,
				null, null, null, null, null, null, null);

			WorkplaceSnapshot merged = workplaceWithoutStack.merge(new WorkplaceSnapshot(
				null, null, null, null, null, null, null, null, newStack));

			assertThat(merged.stack()).isEqualTo(newStack);
		}

		@Test
		@DisplayName("patch에 하위 노드가 없으면 기존 트리를 유지한다")
		void keepsSubtreeWhenPatchOmitsIt() {
			ClientSnapshot merged = existing().merge(emptyPatch());

			assertThat(merged).isEqualTo(existing());
		}
	}
}
