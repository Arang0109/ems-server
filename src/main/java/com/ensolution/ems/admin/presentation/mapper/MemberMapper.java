package com.ensolution.ems.admin.presentation.mapper;

import com.ensolution.ems.admin.application.command.CreateMemberCommand;
import com.ensolution.ems.admin.application.command.UpdateMemberCommand;
import com.ensolution.ems.admin.domain.Member;
import com.ensolution.ems.admin.presentation.request.CreateMemberRequest;
import com.ensolution.ems.admin.presentation.request.UpdateMemberRequest;
import com.ensolution.ems.admin.presentation.response.MemberResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface MemberMapper {
	MemberResponse toResponse(Member member);
	List<MemberResponse> toResponses(List<Member> members);
	
	CreateMemberCommand toCreateMemberCommand(CreateMemberRequest request, Long tenantId);

	UpdateMemberCommand toUpdateMemberCommand(UpdateMemberRequest request, Long id, Long tenantId);
}
