package com.ensolution.ems.admin.presentation.mapper;

import com.ensolution.ems.admin.presentation.request.CreateMemberRequest;
import com.ensolution.ems.admin.presentation.request.UpdateMemberRequest;
import com.ensolution.ems.admin.presentation.response.MemberResponse;
import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.UpdateUserCommand;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * admin의 요청·응답 DTO와 auth가 공개한 {@code port/in} 계약 사이의 변환.
 * <p>
 * 회원은 auth의 {@code User}가 원장이며 이 모듈은 관리 화면의 표현만 갖는다. 그래서 중간 도메인을 두지 않고
 * <b>Request → auth Command</b>, <b>auth {@link UserSummary} → {@link MemberResponse}</b>로 곧장 넘긴다
 * (루트 {@code CLAUDE.md}의 공유 커널 — 포트 시그니처에 이미 드러난 타입을 다시 감싸지 않는다).
 * 문서 관리 쪽 {@code AdminDocumentMapper}와 같은 형태다.
 * <p>
 * {@code unmappedTargetPolicy = ERROR}인 것은 의도다. auth가 {@code UserSummary}에 필드를 더하면
 * 여기서 컴파일이 깨져 응답 계약을 함께 검토하게 된다 — 조용히 누락되는 편보다 낫다.
 */
@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MemberMapper {

	@Mapping(target = "id", source = "userId")
	MemberResponse toResponse(UserSummary summary);

	List<MemberResponse> toResponses(List<UserSummary> summaries);

	@Mapping(target = "tenantId", source = "tenantId")
	CreateUserCommand toCreateCommand(CreateMemberRequest request, Long tenantId);

	@Mapping(target = "userId", source = "userId")
	@Mapping(target = "tenantId", source = "tenantId")
	UpdateUserCommand toUpdateCommand(UpdateMemberRequest request, Long userId, Long tenantId);
}
