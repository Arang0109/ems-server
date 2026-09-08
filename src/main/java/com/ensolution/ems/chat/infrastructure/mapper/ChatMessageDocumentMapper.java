package com.ensolution.ems.chat.infrastructure.mapper;

import com.ensolution.ems.chat.domain.ChatMessage;
import com.ensolution.ems.chat.infrastructure.document.ChatMessageDocument;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ChatMessageDocumentMapper {

	ChatMessageDocument toDocument(ChatMessage message);

	ChatMessage toDomain(ChatMessageDocument document);

	List<ChatMessage> toDomainList(List<ChatMessageDocument> documents);
}
