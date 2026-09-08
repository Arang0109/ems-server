package com.ensolution.ems.chat.infrastructure.mapper;

import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.infrastructure.entity.ChatParticipantEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ChatParticipantEntityMapper {

	@Mapping(target = "participantId", source = "id")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	ChatParticipantEntity toEntity(ChatParticipant participant);

	@Mapping(target = "id", source = "participantId")
	ChatParticipant toDomain(ChatParticipantEntity entity);

	List<ChatParticipant> toDomainList(List<ChatParticipantEntity> entities);
}
