package com.ensolution.ems.auth.infrastructure.mapper;

import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.infrastructure.entity.UserEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder(),
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserEntityMapper {

	// role FK는 Adapter에서 getReferenceById로 연결한다.
	@Mapping(target = "userId", source = "id")
	UserEntity toEntity(User user);

	@Mapping(target = "id", source = "userId")
	@Mapping(target = "roleId", source = "role.roleId")
	User toDomain(UserEntity user);
	
	List<User> toDomainList(List<UserEntity> users);
}
