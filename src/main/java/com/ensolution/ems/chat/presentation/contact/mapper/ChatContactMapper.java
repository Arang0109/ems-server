package com.ensolution.ems.chat.presentation.contact.mapper;

import com.ensolution.ems.chat.application.command.ChatContactListItem;
import com.ensolution.ems.chat.presentation.contact.response.ChatContactResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChatContactMapper {

	ChatContactResponse toResponse(ChatContactListItem item);

	List<ChatContactResponse> toResponses(List<ChatContactListItem> items);
}
