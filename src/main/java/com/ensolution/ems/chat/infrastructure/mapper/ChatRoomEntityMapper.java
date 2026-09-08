package com.ensolution.ems.chat.infrastructure.mapper;

import com.ensolution.ems.chat.domain.ChatRoom;
import com.ensolution.ems.chat.infrastructure.entity.ChatRoomEntity;
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
public interface ChatRoomEntityMapper {

	// createdAt·modifiedAt은 저장 메타라 도메인에 없다. auditing 리스너가 채운다.
	@Mapping(target = "roomId", source = "id")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	ChatRoomEntity toEntity(ChatRoom room);

	@Mapping(target = "id", source = "roomId")
	ChatRoom toDomain(ChatRoomEntity entity);

	List<ChatRoom> toDomainList(List<ChatRoomEntity> entities);
}
