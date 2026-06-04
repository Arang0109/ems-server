package com.ensolution.ems.auth.presentation.mapper;

import com.ensolution.ems.auth.application.command.SignInCommand;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.presentation.request.SignInRequest;
import com.ensolution.ems.auth.presentation.response.SignInResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface SignInRequestMapper {
	SignInCommand toCommand(SignInRequest signInRequest);
	SignInResponse toResponse(SignInResult signInResult);
}
