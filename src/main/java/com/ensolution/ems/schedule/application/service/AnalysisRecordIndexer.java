package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.port.out.AnalysisRecordRepository;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 한 측정계획의 실험분석정보를 측정물질로 찾을 수 있게 색인한다.
 *
 * <p>측정항목(스냅샷)과 분석 결과를 잇는 축은 {@code pollutantId} 하나이고, 그 결합 규칙은
 * 이력 기록({@link MeasurementRecordRecorder})과 성적서 내보내기({@link ScheduleExportAssembler})
 * 양쪽에서 똑같이 필요하다. 규칙이 두 곳으로 갈라지면 같은 계획을 두고 이력과 성적서의 값이
 * 달라질 수 있으므로 여기 한 곳에 둔다.
 *
 * <p>호출하는 서비스의 트랜잭션에 참여하므로 {@code @Transactional}을 붙이지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisRecordIndexer {

	private final AnalysisRecordRepository analysisRecordRepository;

	/**
	 * 계획의 분석 결과를 측정물질로 찾을 수 있게 색인한다.
	 * <p>
	 * 계획 안에서 측정물질은 유일해야 하지만 그 유일성은 등록 시점 검사로만 지켜지므로,
	 * 어긋난 데이터가 호출 경로를 막지 않도록 병합 규칙을 준다.
	 */
	public Map<Long, AnalysisRecord> indexByPollutantId(Long scheduleId, Long tenantId) {
		return analysisRecordRepository.findByScheduleId(scheduleId, tenantId).stream()
			.filter(Objects::nonNull)
			.filter(this::joinable)
			.collect(Collectors.toMap(
				AnalysisRecord::getPollutantId, Function.identity(), this::preferLatest));
	}

	/** 어느 측정물질의 분석인지 알 수 없으면 어느 측정항목에도 붙일 수 없다. */
	private boolean joinable(AnalysisRecord analysis) {
		if (analysis.getPollutantId() != null) return true;
		log.warn("측정물질 식별자가 없는 분석 결과는 색인에서 제외합니다. scheduleId={}, analysisId={}",
			analysis.getScheduleId(), analysis.getId());
		return false;
	}

	/** 조회가 등록순이므로 뒤에 온 것이 나중 기록이다. 다시 입력한 값을 쓴다. */
	private AnalysisRecord preferLatest(AnalysisRecord earlier, AnalysisRecord later) {
		log.warn("같은 측정물질의 분석 결과가 둘 이상이라 나중 기록을 사용합니다. scheduleId={}, pollutantId={}",
			later.getScheduleId(), later.getPollutantId());
		return later;
	}
}
