package com.ensolution.ems.auth.presentation.mapper;

import com.ensolution.ems.auth.application.command.SignUpCommand;
import com.ensolution.ems.auth.presentation.request.SignUpRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface SignUpRequestMapper {
	SignUpCommand toCommand(SignUpRequest signUpRequest);
}
