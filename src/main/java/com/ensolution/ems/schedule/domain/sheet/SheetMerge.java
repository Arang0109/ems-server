package com.ensolution.ems.schedule.domain.sheet;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 저장 요청 시트를 서버 보관본에 병합한다. 시트에는 식별자가 없고 {@code category}가 자연키이므로
 * 카테고리를 키로 삼아 요청에 담긴 시트만 교체하고, 요청에 없는 시트는 서버 값을 그대로 둔다.
 * <p>
 * 전체 배열을 통째로 덮어쓰지 않는 이유는 두 가지다.
 * <ul>
 *   <li>두 사용자가 서로 다른 시트를 나눠 입력하는 경우 서로의 입력을 지우지 않는다.</li>
 *   <li>내가 화면을 연 뒤 다른 사용자가 추가한 시트가 내 저장으로 조용히 사라지지 않는다
 *       (그래서 삭제는 {@code deletedSheets}로 명시받는다).</li>
 * </ul>
 * 같은 시트를 동시에 건드린 경우에만 {@code version} 불일치로 충돌을 판정해 저장을 거부한다.
 */
public final class SheetMerge {

	private SheetMerge() {
	}

	/**
	 * @param current  서버 보관본
	 * @param incoming 저장 요청 시트(각 시트는 읽어간 시점의 version을 지닌다)
	 * @param deleted  삭제 대상 시트 참조
	 * @return 병합 결과. 요청에 담긴 시트는 version이 1 올라간다.
	 * @throws CustomException 요청이 읽어간 뒤 서버 시트가 먼저 바뀐 경우(SCHEDULE_SHEET_VERSION_CONFLICT)
	 */
	public static List<MeasurementSheet> merge(List<MeasurementSheet> current,
	                                           List<MeasurementSheet> incoming,
	                                           List<SheetRef> deleted) {
		Map<MeasurementCategory, MeasurementSheet> merged = indexByCategory(current);
		List<MeasurementSheet> requested = incoming == null ? List.of() : incoming;
		List<SheetRef> removals = deleted == null ? List.of() : deleted;

		requireNoConflict(merged, requested, removals);

		for (SheetRef ref : removals) {
			merged.remove(ref.category());
		}
		// LinkedHashMap.put은 기존 키의 자리를 유지하므로 시트 순서가 저장할 때마다 뒤바뀌지 않는다.
		for (MeasurementSheet sheet : requested) {
			merged.put(sheet.getCategory(), sheet.withNextVersion());
		}
		return new ArrayList<>(merged.values());
	}

	/** 요청이 건드리는 시트 중 서버에서 이미 바뀐 것이 있으면 어느 시트인지 밝혀 거부한다. */
	private static void requireNoConflict(Map<MeasurementCategory, MeasurementSheet> current,
	                                      List<MeasurementSheet> incoming,
	                                      List<SheetRef> deleted) {
		List<String> conflicted = new ArrayList<>();

		for (MeasurementSheet sheet : incoming) {
			if (isStale(current.get(sheet.getCategory()), sheet.getVersion())) {
				conflicted.add(sheet.getCategory().getDescription());
			}
		}
		for (SheetRef ref : deleted) {
			if (isStale(current.get(ref.category()), ref.version())) {
				conflicted.add(ref.category().getDescription());
			}
		}
		if (conflicted.isEmpty()) return;

		throw new CustomException(
			ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT,
			String.format("다른 사용자가 %s 측정 데이터를 먼저 저장했습니다. 최신 내용을 불러온 뒤 다시 저장해 주세요.",
				String.join("·", conflicted.stream().distinct().toList()))
		);
	}

	/**
	 * 요청이 들고 온 version이 서버 보관본과 어긋나는지 판정한다.
	 * 서버에 없던 카테고리(신규 시트)와 버전 도입 전에 저장된 레거시 시트는 충돌로 보지 않는다.
	 */
	private static boolean isStale(MeasurementSheet currentSheet, Long incomingVersion) {
		if (currentSheet == null || currentSheet.getVersion() == null) return false;
		return !currentSheet.getVersion().equals(incomingVersion);
	}

	private static Map<MeasurementCategory, MeasurementSheet> indexByCategory(List<MeasurementSheet> sheets) {
		Map<MeasurementCategory, MeasurementSheet> indexed = new LinkedHashMap<>();
		if (sheets == null) return indexed;

		for (MeasurementSheet sheet : sheets) {
			if (sheet.getCategory() == null) {
				throw new CustomException(ErrorCode.VALUE_MUST_NOT_BE_NULL, "측정 시트의 측정 카테고리는 필수 값입니다.");
			}
			indexed.put(sheet.getCategory(), sheet);
		}
		return indexed;
	}
}
