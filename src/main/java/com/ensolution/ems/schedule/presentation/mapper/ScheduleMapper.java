package com.ensolution.ems.schedule.presentation.mapper;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetCandidate;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetDetail;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.ChangeScheduleEquipmentsCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeClientSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateBasicInfoCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleItemCommand;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleEquipmentsRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeClientSnapshotRequest;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateBasicInfoRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleItemRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleRequest;
import com.ensolution.ems.schedule.presentation.response.PreviousSheetCandidateResponse;
import com.ensolution.ems.schedule.presentation.response.PreviousSheetResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleListResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleResponse;
import com.ensolution.ems.schedule.presentation.response.snapshot.ScheduleSnapshotResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface ScheduleMapper {

	@Mapping(target = "tenantId", source = "tenantId")
	@Mapping(target = "registeredBy", source = "registeredBy")
	CreateScheduleCommand toCreateCommand(CreateScheduleRequest request, Long tenantId, Long registeredBy);

	UpdateScheduleCommand toUpdateCommand(UpdateScheduleRequest request);

	ChangeScheduleEquipmentsCommand toChangeEquipmentsCommand(ChangeScheduleEquipmentsRequest request);

	ChangeClientSnapshotCommand toChangeClientCommand(ChangeClientSnapshotRequest request);

	UpdateBasicInfoCommand toUpdateBasicInfoCommand(UpdateBasicInfoRequest request);

	UpdateScheduleItemCommand toUpdateItemCommand(UpdateScheduleItemRequest request);

	@Mapping(target = "id", source = "meta.id")
	@Mapping(target = "tenantId", source = "meta.tenantId")
	@Mapping(target = "stackId", source = "meta.stackId")
	@Mapping(target = "teamId", source = "meta.teamId")
	@Mapping(target = "measurementField", source = "meta.measurementField")
	@Mapping(target = "sampledAt", source = "meta.sampledAt")
	@Mapping(target = "schedulePurpose", source = "meta.schedulePurpose")
	@Mapping(target = "status", source = "meta.status")
	@Mapping(target = "referenceNumber", source = "meta.referenceNumber")
	@Mapping(target = "createdAt", source = "meta.createdAt")
	@Mapping(target = "modifiedAt", source = "meta.modifiedAt")
	@Mapping(target = "snapshot", source = "snapshot")
	ScheduleResponse toResponse(ScheduleDetail detail);

	/**
	 * 세부 스냅샷을 응답으로 변환한다. 문서의 저장 메타(id·scheduleId·tenantId·status·version·createdAt)는
	 * 대상 record에 자리가 없어 그대로 빠진다 — 최상위 메타가 진실의 원천이므로 사본을 함께 내보내지 않는다.
	 * 하위 트리(client→workplace→stack→facilities·preventions)의 변환 메서드는 MapStruct가 이름 기준으로
	 * 생성하므로 따로 선언하지 않는다.
	 */
	ScheduleSnapshotResponse toSnapshotResponse(ScheduleSnapshot snapshot);

	List<ScheduleListResponse> toListResponses(List<ScheduleListItem> items);

	/** 불러올 이전 기록지가 없으면 null이 그대로 내려간다(첫 회차이거나 그 기록지를 처음 쓰는 경우). */
	PreviousSheetResponse toPreviousSheetResponse(PreviousSheetDetail detail);

	List<PreviousSheetCandidateResponse> toPreviousSheetCandidateResponses(List<PreviousSheetCandidate> candidates);
}
