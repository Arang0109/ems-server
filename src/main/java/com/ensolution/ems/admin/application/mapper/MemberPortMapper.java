package com.ensolution.ems.admin.application.mapper;

import com.ensolution.ems.admin.application.command.CreateMemberCommand;
import com.ensolution.ems.admin.application.command.UpdateMemberCommand;
import com.ensolution.ems.admin.domain.Member;
import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.UpdateUserCommand;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface MemberPortMapper {
	@Mapping(source = "userId", target = "id")
	Member toMember(UserSummary userSummary);
	List<Member> toMemberList(List<UserSummary> userSummaries);

	CreateUserCommand toCreateUserCommand(CreateMemberCommand command);

	@Mapping(source = "id", target = "userId")
	UpdateUserCommand toUpdateUserCommand(UpdateMemberCommand command);
}
