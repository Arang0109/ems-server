package com.ensolution.ems.schedule.presentation.analysis.mapper;

import com.ensolution.ems.schedule.application.command.update.SaveAnalysisResultsCommand;
import com.ensolution.ems.schedule.application.command.update.SaveSamplingTimesCommand;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveAnalysisResultsRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveSamplingTimesRequest;
import com.ensolution.ems.schedule.presentation.analysis.response.AnalysisResultResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 실험분석정보 요청·응답 매퍼.
 *
 * <p>응답의 출처는 측정항목 스냅샷 하나다 — 판정 근거는 항목이, 분석 결과는 그 안의
 * {@code analysis}가 갖는다. 아직 분석 전이면 {@code analysis}가 null이고 결과 칸은 모두 비어 나간다.
 */
@Mapper(
	componentModel = "spring",
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AnalysisResultMapper {

	SaveSamplingTimesCommand toSaveSamplingTimesCommand(SaveSamplingTimesRequest request);

	SaveAnalysisResultsCommand toSaveAnalysisResultsCommand(SaveAnalysisResultsRequest request);

	@Mapping(target = "pollutantName", source = "nameKr")
	@Mapping(target = "analysisValue", source = "analysis.analysisValue")
	@Mapping(target = "unit", source = "analysis.unit")
	@Mapping(target = "analysisMethod", source = "analysis.analysisMethod")
	@Mapping(target = "analysisEquipment", source = "analysis.analysisEquipment")
	@Mapping(target = "samplingStartedAt", source = "analysis.samplingStartedAt")
	@Mapping(target = "samplingEndedAt", source = "analysis.samplingEndedAt")
	AnalysisResultResponse toResponse(SamplingItemSnapshot item);

	List<AnalysisResultResponse> toResponses(List<SamplingItemSnapshot> items);
}
