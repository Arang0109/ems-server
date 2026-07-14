package com.ensolution.ems.admin.application.service;

import com.ensolution.ems.admin.application.command.CreateMemberCommand;
import com.ensolution.ems.admin.application.command.UpdateMemberCommand;
import com.ensolution.ems.admin.application.mapper.MemberPortMapper;
import com.ensolution.ems.admin.domain.Member;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

	private final UserQueryUseCase userQueryUseCase;
	private final UserCommandUseCase userCommandUseCase;

	private final MemberPortMapper memberPortMapper;

	public void createMember(CreateMemberCommand command) {
		userCommandUseCase.createUser(memberPortMapper.toCreateUserCommand(command));
	}

	public void updateMember(UpdateMemberCommand command) {
		userCommandUseCase.updateUser(memberPortMapper.toUpdateUserCommand(command));
	}

	public void deleteMember(Long id) {
		userCommandUseCase.deleteUser(id);
	}

	@Transactional(readOnly = true)
	public Member getMember(Long id) {
		return memberPortMapper.toMember(userQueryUseCase.getUser(id));
	}

	@Transactional(readOnly = true)
	public List<Member> getMemberList(Long tenantId) {
		List<UserSummary> userSummaryList = userQueryUseCase.getUserList(tenantId);
		return memberPortMapper.toMemberList(userSummaryList);
	}
}
