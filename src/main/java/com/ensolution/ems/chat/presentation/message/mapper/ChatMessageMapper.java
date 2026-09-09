package com.ensolution.ems.chat.presentation.message.mapper;

import com.ensolution.ems.chat.application.command.AttachmentUpload;
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

	/** 텍스트 전송. 첨부는 없다 */
	@Mapping(target = "content", source = "request.content")
	@Mapping(target = "clientMessageId", source = "request.clientMessageId")
	@Mapping(target = "attachment", ignore = true)
	SendMessageCommand toSendCommand(SendMessageRequest request, Long roomId, Long senderId, Long tenantId);

	/**
	 * 첨부 전송. 표현이 둘(JSON 본문·multipart)일 뿐 유스케이스는 하나이므로 같은 Command 로 모은다.
	 * 본문은 캡션이라 없어도 된다.
	 * <p>
	 * {@code content} 를 명시적으로 지정하는 이유: {@link AttachmentUpload} 에도 {@code content}
	 * (파일 바이트)가 있어 이름만으로는 어느 쪽인지 정해지지 않는다.
	 */
	@Mapping(target = "content", source = "content")
	@Mapping(target = "clientMessageId", source = "clientMessageId")
	@Mapping(target = "attachment", source = "attachment")
	SendMessageCommand toSendCommand(String content, String clientMessageId, AttachmentUpload attachment,
		Long roomId, Long senderId, Long tenantId);

	@Mapping(target = "lastReadMessageId", source = "request.lastReadMessageId")
	MarkAsReadCommand toMarkAsReadCommand(MarkAsReadRequest request, Long roomId, Long readerId, Long tenantId);

	ChatMessageResponse toResponse(ChatMessageListItem item);

	List<ChatMessageResponse> toResponses(List<ChatMessageListItem> items);

	ChatMessagePageResponse toPageResponse(ChatMessagePage page);
}
