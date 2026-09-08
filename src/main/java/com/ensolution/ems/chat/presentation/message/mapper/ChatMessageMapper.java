package com.ensolution.ems.chat.presentation.message.mapper;

import com.ensolution.ems.chat.application.command.ChatMessageListItem;
import com.ensolution.ems.chat.application.command.ChatMessagePage;
import com.ensolution.ems.chat.application.command.MarkAsReadCommand;
import com.ensolution.ems.chat.application.command.SendMessageCommand;
import com.ensolution.ems.chat.presentation.message.request.SendMessageRequest;
import com.ensolution.ems.chat.presentation.message.response.ChatMessagePageResponse;
import com.ensolution.ems.chat.presentation.message.response.ChatMessageResponse;
import com.ensolution.ems.chat.presentation.room.request.MarkAsReadRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChatMessageMapper {

	@Mapping(target = "content", source = "request.content")
	@Mapping(target = "clientMessageId", source = "request.clientMessageId")
	SendMessageCommand toSendCommand(SendMessageRequest request, Long roomId, Long senderId, Long tenantId);

	@Mapping(target = "lastReadMessageId", source = "request.lastReadMessageId")
	MarkAsReadCommand toMarkAsReadCommand(MarkAsReadRequest request, Long roomId, Long readerId, Long tenantId);

	ChatMessageResponse toResponse(ChatMessageListItem item);

	List<ChatMessageResponse> toResponses(List<ChatMessageListItem> items);

	ChatMessagePageResponse toPageResponse(ChatMessagePage page);
}
