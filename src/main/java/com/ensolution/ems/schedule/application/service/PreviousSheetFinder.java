package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.detail.PreviousSheetCandidate;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetDetail;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.SheetReuse;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 새 기록지를 채울 이전 회차 시트를 찾는다.
 *
 * <p><b>직전 회차만 보지 않는다.</b> 같은 측정시설이라도 회차마다 쓰는 기록지 조합이 달라지므로
 * (먼지·수은을 함께 쓰다가 이번엔 먼지만), 직전 회차에 그 기록지가 없는 경우가 오히려 흔하다.
 * 직전 한 건만 보면 "이전 기록이 없다"는 답이 자주 나와 기능이 무용해진다.
 * 그래서 최근 완료 회차를 거슬러 올라가며 <b>그 기록지를 실제로 쓴</b> 회차를 찾는다.
 *
 * <p>가장 최근 한 건이 늘 최선인 것도 아니다 — 직전 회차가 이상 조업이었거나 값이 어긋난 회차일 수
 * 있어, 현장은 "그 전 정상 회차"를 출발점으로 삼고 싶어 한다. 그래서 후보를 목록으로도 내주고
 * ({@link #findCandidates}) 사용자가 고른 회차를 지정해 받을 수 있게 한다.
 *
 * <p>거슬러 올라가는 깊이는 {@value #MAX_LOOKBACK}회로 제한한다. 세부 스냅샷이 문서 저장소에 있어
 * 회차마다 한 번씩 읽어야 하므로 무제한 탐색은 조회량이 예측되지 않는다. 그만큼 거슬러 올라가도
 * 없다면 그 기록지는 사실상 처음 쓰는 것이다.
 */
@Component
@RequiredArgsConstructor
public class PreviousSheetFinder {

	/** 기록지를 찾아 거슬러 올라갈 최대 회차 수. */
	private static final int MAX_LOOKBACK = 10;

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;

	/**
	 * 같은 측정시설에서 이 계획보다 앞서 완료된 회차 중, 해당 기록지를 쓴 가장 최근 시트를 반환한다.
	 * 불러올 기록이 없으면 null이다 — 첫 회차이거나 그 기록지를 처음 쓰는 경우이며 오류가 아니다.
	 */
	public PreviousSheetDetail find(Schedule current, MeasurementCategory category) {
		return find(current, category, null);
	}

	/**
	 * 출처 회차를 지정해 시트를 반환한다. {@code sourceScheduleId}가 null이면 가장 최근 회차를 쓴다.
	 *
	 * <p>지정한 회차가 후보 범위(최근 완료 {@value #MAX_LOOKBACK}회) 밖이거나 그 사이 재개방되어
	 * 완료가 아니게 됐으면 null이다. 화면이 들고 있던 후보 목록이 낡았다는 뜻이며, 다시 조회하면 된다.
	 */
	public PreviousSheetDetail find(Schedule current, MeasurementCategory category, Long sourceScheduleId) {
		for (Schedule candidate : lookback(current)) {
			if (sourceScheduleId != null && !sourceScheduleId.equals(candidate.getId())) continue;

			MeasurementSheet sheet = sheetOf(candidate, category);
			if (sheet == null) continue;

			return new PreviousSheetDetail(
				candidate.getId(),
				candidate.getSampledAt(),
				candidate.getReferenceNumber(),
				SheetReuse.forNextRound(sheet));
		}
		return null;
	}

	/**
	 * 해당 기록지를 실제로 쓴 이전 완료 회차를 최신순으로 모두 반환한다. 시트 본문은 담지 않는다 —
	 * 목록은 어느 회차인지만 보여주면 되고, 후보마다 시트를 실어 보내면 응답이 불필요하게 커진다.
	 */
	public List<PreviousSheetCandidate> findCandidates(Schedule current, MeasurementCategory category) {
		List<PreviousSheetCandidate> candidates = new ArrayList<>();
		for (Schedule candidate : lookback(current)) {
			if (sheetOf(candidate, category) == null) continue;

			candidates.add(new PreviousSheetCandidate(
				candidate.getId(), candidate.getSampledAt(), candidate.getReferenceNumber()));
		}
		return candidates;
	}

	private List<Schedule> lookback(Schedule current) {
		return scheduleRepository.findRecentCompletedBefore(
			current.getStackId(), current.getTenantId(), current.getSampledAt(), MAX_LOOKBACK);
	}

	private MeasurementSheet sheetOf(Schedule schedule, MeasurementCategory category) {
		ScheduleSnapshot snapshot =
			scheduleDocumentRepository.findByScheduleId(schedule.getId(), schedule.getTenantId());
		if (snapshot == null || snapshot.sheets() == null) return null;

		return snapshot.sheets().stream()
			.filter(sheet -> sheet.getCategory() == category)
			.findFirst()
			.orElse(null);
	}
}
