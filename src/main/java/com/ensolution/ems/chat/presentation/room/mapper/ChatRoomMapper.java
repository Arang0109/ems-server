package com.ensolution.ems.chat.presentation.room.mapper;

import com.ensolution.ems.chat.application.command.ChatRoomDetail;
import com.ensolution.ems.chat.application.command.ChatRoomListItem;
import com.ensolution.ems.chat.application.command.OpenDirectRoomCommand;
import com.ensolution.ems.chat.presentation.room.request.OpenDirectRoomRequest;
import com.ensolution.ems.chat.presentation.room.response.ChatRoomListResponse;
import com.ensolution.ems.chat.presentation.room.response.ChatRoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChatRoomMapper {

	@Mapping(target = "tenantId", source = "tenantId")
	@Mapping(target = "requesterId", source = "requesterId")
	@Mapping(target = "counterpartId", source = "request.counterpartId")
	OpenDirectRoomCommand toOpenCommand(OpenDirectRoomRequest request, Long requesterId, Long tenantId);

	ChatRoomResponse toResponse(ChatRoomDetail detail);

	ChatRoomListResponse toListResponse(ChatRoomListItem item);

	List<ChatRoomListResponse> toListResponses(List<ChatRoomListItem> items);
}
