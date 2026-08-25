package com.ensolution.ems.schedule.domain.history;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 측정주기가 한 해를 나누는 구간 하나. 예) 분기 주기의 {@code 2026-Q1}.
 *
 * <p><b>역년 고정</b>이다 — 구간은 매년 1월에 시작하며 시설·항목마다 다른 기준월을 갖지 않는다.
 * 법정 자가측정이 역년 기준으로 부과되므로 구간을 시설별로 굴리면 오히려 규정과 어긋난다.
 *
 * <p>"월 2회"({@link MeasurementCycle#TWICE_MONTHLY})는 구간이 아니라 <b>한 구간에 필요한 횟수</b>다.
 * 모든 주기를 {@code (구간 수, 구간당 필요 횟수)} 쌍으로 환원했기 때문에, 이행 판정이 주기 종류와
 * 무관하게 {@code 기록 수 >= requiredCount()} 한 식으로 끝난다.
 *
 * <p>이 계산을 {@link MeasurementCycle}(global enum)에 두지 않은 이유: global에는 기능성 코드를 두지
 * 않으며, 무엇이 한 구간인지는 측정 이력을 소유한 이 모듈의 업무 규칙이다. 훗날 "회계연도 기준" 같은
 * 변형이 들어올 자리도 여기다. {@code equipment}의 {@code InspectionItem.nextDueDate()}가 enum이 아니라
 * 도메인 record에 주기 계산을 둔 것과 같은 판단이다.
 *
 * @param index 1부터 시작하는 구간 번호. 상한은 주기별 구간 수
 */
public record MeasurementPeriod(MeasurementCycle cycle, int year, int index) {

	private static final int MONTHS_IN_YEAR = 12;

	public MeasurementPeriod {
		if (cycle == null) {
			throw new IllegalArgumentException("측정주기는 필수입니다.");
		}
		int max = periodsPerYear(cycle);
		if (index < 1 || index > max) {
			throw new IllegalArgumentException(
				"%s 주기의 구간 번호는 1~%d 범위여야 합니다: %d".formatted(cycle, max, index));
		}
	}

	/** 측정일이 속한 구간. 주기가 지정되지 않은 항목은 구간을 특정할 수 없으므로 null이다. */
	public static MeasurementPeriod of(MeasurementCycle cycle, LocalDate date) {
		if (cycle == null || date == null) {
			return null;
		}
		int index = (date.getMonthValue() - 1) / monthsPerPeriod(cycle) + 1;
		return new MeasurementPeriod(cycle, date.getYear(), index);
	}

	/** 해당 연도의 전 구간을 번호 순으로 펼친다. 이행 현황판의 열(column)이 된다. */
	public static List<MeasurementPeriod> allIn(MeasurementCycle cycle, int year) {
		if (cycle == null) {
			return List.of();
		}
		return IntStream.rangeClosed(1, periodsPerYear(cycle))
			.mapToObj(index -> new MeasurementPeriod(cycle, year, index))
			.toList();
	}

	/** 연간 구간 수. */
	public static int periodsPerYear(MeasurementCycle cycle) {
		return switch (cycle) {
			case MONTHLY, TWICE_MONTHLY -> 12;
			case BIMONTHLY -> 6;
			case QUARTERLY -> 4;
			case SEMI_ANNUAL -> 2;
			case ANNUAL -> 1;
		};
	}

	/** 한 구간을 이행하는 데 필요한 측정 횟수. */
	public static int requiredCount(MeasurementCycle cycle) {
		return cycle == MeasurementCycle.TWICE_MONTHLY ? 2 : 1;
	}

	/**
	 * 구간 식별 문자열. 예) {@code 2026-M03}, {@code 2026-T03}, {@code 2026-B2}, {@code 2026-Q1},
	 * {@code 2026-H1}, {@code 2026-Y}
	 *
	 * <p>주기 표식을 포함하는 이유는 원장의 주기가 도중에 바뀔 수 있기 때문이다. 접두가 없으면 같은
	 * {@code (연도, 번호)}가 월 주기와 격월 주기에서 서로 다른 뜻인데도 같은 키가 되어, 과거 기록이
	 * 새 주기의 엉뚱한 칸에 붙는다. 월 주기와 월 2회 주기도 구간은 같지만 필요 횟수가 달라 구분한다.
	 */
	public String key() {
		return switch (cycle) {
			case MONTHLY -> "%d-M%02d".formatted(year, index);
			case TWICE_MONTHLY -> "%d-T%02d".formatted(year, index);
			case BIMONTHLY -> "%d-B%d".formatted(year, index);
			case QUARTERLY -> "%d-Q%d".formatted(year, index);
			case SEMI_ANNUAL -> "%d-H%d".formatted(year, index);
			case ANNUAL -> "%d-Y".formatted(year);
		};
	}

	/** 화면 표시용 이름. 예) {@code 3월}, {@code 1-2월}, {@code 1분기}, {@code 상반기}, {@code 연간} */
	public String label() {
		return switch (cycle) {
			case MONTHLY, TWICE_MONTHLY -> "%d월".formatted(index);
			case BIMONTHLY -> "%d-%d월".formatted(index * 2 - 1, index * 2);
			case QUARTERLY -> "%d분기".formatted(index);
			case SEMI_ANNUAL -> index == 1 ? "상반기" : "하반기";
			case ANNUAL -> "연간";
		};
	}

	/** 구간 시작일(포함). */
	public LocalDate startDate() {
		return LocalDate.of(year, (index - 1) * monthsPerPeriod(cycle) + 1, 1);
	}

	/** 구간 종료일(포함). 이행 기한이며 미이행 경과 판정의 기준이다. */
	public LocalDate endDate() {
		return startDate().plusMonths(monthsPerPeriod(cycle)).minusDays(1);
	}

	/** 이 구간을 이행하는 데 필요한 측정 횟수. */
	public int requiredCount() {
		return requiredCount(cycle);
	}

	/** 기준일 시점에 이행 기한이 이미 지났는지 여부. */
	public boolean isOverdueAt(LocalDate baseDate) {
		return baseDate != null && baseDate.isAfter(endDate());
	}

	private static int monthsPerPeriod(MeasurementCycle cycle) {
		return MONTHS_IN_YEAR / periodsPerYear(cycle);
	}
}
