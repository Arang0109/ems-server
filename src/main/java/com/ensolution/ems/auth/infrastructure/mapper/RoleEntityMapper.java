package com.ensolution.ems.auth.infrastructure.mapper;

import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.infrastructure.entity.RoleEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		builder = @Builder(),
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleEntityMapper {
	Role toDomain(RoleEntity roleEntity);
}
