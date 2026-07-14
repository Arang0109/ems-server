package com.ensolution.ems.auth.presentation.mapper;

import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.presentation.response.RoleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface RoleMapper {
	RoleResponse toResponse(Role role);
	List<RoleResponse> toResponses(List<Role> roles);
}