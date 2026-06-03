package com.ensolution.ems.auth.infrastructure.mapper;

import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.infrastructure.JpaUserEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		builder = @Builder(),
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserDomainEntityMapper {
	JpaUserEntity toEntity(User user);
	User toDomain(JpaUserEntity user);
}