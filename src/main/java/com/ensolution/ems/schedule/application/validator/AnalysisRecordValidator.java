package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.AnalysisRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 실험분석정보 비즈니스 규칙 검증. 포트 조회가 필요한 규칙만 담당한다. */
@Component
@RequiredArgsConstructor
public class AnalysisRecordValidator {

	private final AnalysisRecordRepository analysisRecordRepository;

	/**
	 * 같은 측정계획에 같은 측정물질의 분석 기록이 이미 없는지 확인한다.
	 * 한 회차의 한 항목에는 확정된 분석값이 하나뿐이어야 하므로, 재분석은 등록이 아니라 수정으로 처리한다.
	 */
	public void requireUniquePollutant(Long scheduleId, Long tenantId, Long pollutantId) {
		if (analysisRecordRepository.existsByScheduleIdAndPollutantId(scheduleId, tenantId, pollutantId)) {
			throw new CustomException(ErrorCode.SCHEDULE_ANALYSIS_ALREADY_EXISTS);
		}
	}

	/**
	 * 일괄 저장 요청 안에서 같은 측정항목이 두 번 담기지 않았는지 확인한다.
	 * 한 항목에 서로 다른 채취시간이 실려 오면 어느 쪽이 맞는지 정할 근거가 없으므로,
	 * 뒤엣것으로 조용히 덮어쓰는 대신 요청 전체를 거절해 사용자가 표를 고치게 한다.
	 */
	public void requireUniquePollutants(List<Long> pollutantIds) {
		Set<Long> seen = new HashSet<>();
		for (Long pollutantId : pollutantIds) {
			if (Objects.nonNull(pollutantId) && !seen.add(pollutantId)) {
				throw new CustomException(ErrorCode.SCHEDULE_ANALYSIS_DUPLICATE_ITEM);
			}
		}
	}
}
