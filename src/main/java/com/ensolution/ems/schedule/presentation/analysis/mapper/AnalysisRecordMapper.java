package com.ensolution.ems.schedule.presentation.analysis.mapper;

import com.ensolution.ems.schedule.application.command.create.CreateAnalysisRecordCommand;
import com.ensolution.ems.schedule.application.command.update.SaveAnalysisResultsCommand;
import com.ensolution.ems.schedule.application.command.update.SaveSamplingTimesCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateAnalysisRecordCommand;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.presentation.analysis.request.CreateAnalysisRecordRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveAnalysisResultsRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveSamplingTimesRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.UpdateAnalysisRecordRequest;
import com.ensolution.ems.schedule.presentation.analysis.response.AnalysisRecordResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AnalysisRecordMapper {

	CreateAnalysisRecordCommand toCreateCommand(CreateAnalysisRecordRequest request);

	UpdateAnalysisRecordCommand toUpdateCommand(UpdateAnalysisRecordRequest request);

	SaveSamplingTimesCommand toSaveSamplingTimesCommand(SaveSamplingTimesRequest request);

	SaveAnalysisResultsCommand toSaveAnalysisResultsCommand(SaveAnalysisResultsRequest request);

	AnalysisRecordResponse toResponse(AnalysisRecord analysisRecord);

	List<AnalysisRecordResponse> toResponses(List<AnalysisRecord> analysisRecords);
}
