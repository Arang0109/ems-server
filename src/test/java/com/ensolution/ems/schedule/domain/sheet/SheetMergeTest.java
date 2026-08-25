package com.ensolution.ems.schedule.domain.sheet;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 동시 편집 병합 규칙 검증.
 * 핵심 계약은 세 가지다 — 요청에 없는 시트는 건드리지 않고, 같은 시트를 먼저 저장한 요청이 있으면
 * 거부하며, 서로 다른 시트를 고친 두 저장은 양쪽 다 남는다.
 */
class SheetMergeTest {

	private static MeasurementSheet sheet(MeasurementCategory category, Long version) {
		return MeasurementSheet.builder()
			.category(category)
			.version(version)
			.build();
	}

	/** 같은 카테고리라도 내용이 달라졌는지 구분할 수 있도록 표식을 남긴다. */
	private static MeasurementSheet sheet(MeasurementCategory category, Long version, String marker) {
		return MeasurementSheet.builder()
			.category(category)
			.version(version)
			.avgTm(new BigDecimal(marker))
			.build();
	}

	private static MeasurementSheet find(List<MeasurementSheet> sheets, MeasurementCategory category) {
		return sheets.stream().filter(s -> s.getCategory() == category).findFirst().orElse(null);
	}

	@Nested
	@DisplayName("병합")
	class Merge {

		@Test
		@DisplayName("요청에 담긴 시트만 교체하고 나머지는 보관본을 유지한다")
		void keepsUntouchedSheets() {
			List<MeasurementSheet> current = List.of(sheet(MeasurementCategory.GAS, 1L, "1"),
				sheet(MeasurementCategory.DUST, 2L, "2"));

			List<MeasurementSheet> merged = SheetMerge.merge(
				current, List.of(sheet(MeasurementCategory.GAS, 1L, "9")), List.of());

			assertThat(merged).hasSize(2);
			assertThat(find(merged, MeasurementCategory.GAS).getAvgTm()).isEqualByComparingTo("9");
			assertThat(find(merged, MeasurementCategory.DUST).getAvgTm()).isEqualByComparingTo("2");
		}

		@Test
		@DisplayName("저장한 시트의 version을 올린다")
		void bumpsVersion() {
			List<MeasurementSheet> merged = SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, 4L)),
				List.of(sheet(MeasurementCategory.GAS, 4L)),
				List.of());

			assertThat(find(merged, MeasurementCategory.GAS).getVersion()).isEqualTo(5L);
		}

		@Test
		@DisplayName("신규 시트는 version 0에서 시작한다")
		void startsNewSheetAtZero() {
			List<MeasurementSheet> merged = SheetMerge.merge(
				List.of(), List.of(sheet(MeasurementCategory.MERCURY, null)), List.of());

			assertThat(find(merged, MeasurementCategory.MERCURY).getVersion()).isZero();
		}

		@Test
		@DisplayName("삭제 목록의 시트는 제거한다")
		void removesDeletedSheets() {
			List<MeasurementSheet> merged = SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, 1L), sheet(MeasurementCategory.DUST, 1L)),
				List.of(sheet(MeasurementCategory.GAS, 1L)),
				List.of(new SheetRef(MeasurementCategory.DUST, 1L)));

			assertThat(merged).extracting(MeasurementSheet::getCategory)
				.containsExactly(MeasurementCategory.GAS);
		}

		@Test
		@DisplayName("기존 시트를 다시 저장해도 순서가 바뀌지 않는다")
		void keepsOrder() {
			List<MeasurementSheet> current = List.of(sheet(MeasurementCategory.GAS, 1L),
				sheet(MeasurementCategory.DUST, 1L), sheet(MeasurementCategory.MERCURY, 1L));

			List<MeasurementSheet> merged = SheetMerge.merge(
				current, List.of(sheet(MeasurementCategory.DUST, 1L)), List.of());

			assertThat(merged).extracting(MeasurementSheet::getCategory).containsExactly(
				MeasurementCategory.GAS, MeasurementCategory.DUST, MeasurementCategory.MERCURY);
		}

		@Test
		@DisplayName("서로 다른 시트를 고친 두 저장은 양쪽 다 남는다")
		void concurrentEditsOnDifferentSheetsBothSurvive() {
			List<MeasurementSheet> server = List.of(sheet(MeasurementCategory.GAS, 0L, "0"),
				sheet(MeasurementCategory.DUST, 0L, "0"));

			// A가 가스상을 저장한 뒤, 같은 화면을 열고 있던 B가 먼지를 저장한다.
			List<MeasurementSheet> afterA = SheetMerge.merge(
				server, List.of(sheet(MeasurementCategory.GAS, 0L, "1")), List.of());
			List<MeasurementSheet> afterB = SheetMerge.merge(
				afterA, List.of(sheet(MeasurementCategory.DUST, 0L, "2")), List.of());

			assertThat(find(afterB, MeasurementCategory.GAS).getAvgTm()).isEqualByComparingTo("1");
			assertThat(find(afterB, MeasurementCategory.DUST).getAvgTm()).isEqualByComparingTo("2");
		}
	}

	@Nested
	@DisplayName("충돌 판정")
	class Conflict {

		@Test
		@DisplayName("읽어간 뒤 같은 시트가 먼저 저장됐으면 거부한다")
		void rejectsStaleSheet() {
			assertThatThrownBy(() -> SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, 5L)),
				List.of(sheet(MeasurementCategory.GAS, 4L)),
				List.of()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT);
		}

		@Test
		@DisplayName("어느 시트가 어긋났는지 밝힌다")
		void namesConflictedSheet() {
			assertThatThrownBy(() -> SheetMerge.merge(
				List.of(sheet(MeasurementCategory.DUST, 2L)),
				List.of(sheet(MeasurementCategory.DUST, 1L)),
				List.of()))
				.hasMessageContaining(MeasurementCategory.DUST.getDescription());
		}

		@Test
		@DisplayName("지우려는 사이 다른 사용자가 그 시트를 저장했으면 거부한다")
		void rejectsStaleDeletion() {
			assertThatThrownBy(() -> SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, 3L)),
				List.of(),
				List.of(new SheetRef(MeasurementCategory.GAS, 2L))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT);
		}

		@Test
		@DisplayName("같은 카테고리를 두 사람이 각자 새로 만들었으면 거부한다")
		void rejectsDuplicateNewSheet() {
			assertThatThrownBy(() -> SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, 0L)),
				List.of(sheet(MeasurementCategory.GAS, null)),
				List.of()))
				.isInstanceOf(CustomException.class);
		}

		@Test
		@DisplayName("버전 도입 전에 저장된 시트는 판정 대상이 아니다")
		void skipsLegacySheet() {
			List<MeasurementSheet> merged = SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, null)),
				List.of(sheet(MeasurementCategory.GAS, null)),
				List.of());

			assertThat(find(merged, MeasurementCategory.GAS).getVersion()).isZero();
		}

		@Test
		@DisplayName("이미 삭제된 시트를 다시 지우는 것은 충돌이 아니다")
		void allowsRepeatedDeletion() {
			assertThatCode(() -> SheetMerge.merge(
				List.of(sheet(MeasurementCategory.GAS, 1L)),
				List.of(sheet(MeasurementCategory.GAS, 1L)),
				List.of(new SheetRef(MeasurementCategory.DUST, 2L))))
				.doesNotThrowAnyException();
		}
	}

	@Test
	@DisplayName("보관본과 요청이 비어 있어도 빈 결과를 낸다")
	void handlesNulls() {
		assertThat(SheetMerge.merge(null, null, null)).isEmpty();
	}
}
