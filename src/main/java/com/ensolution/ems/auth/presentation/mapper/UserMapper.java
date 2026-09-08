package com.ensolution.ems.auth.presentation.mapper;

import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.auth.presentation.response.UserListResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * auth의 조회 VO({@link UserSummary})를 전체 사용자용 응답으로 옮긴다.
 * <p>
 * {@code UserSummary}의 필드 중 일부만 쓰는 것이 의도다 — 무엇을 빼는지와 그 이유는
 * {@link UserListResponse}의 javadoc에 있다. source 쪽 미사용 필드는 MapStruct가 조용히 무시하므로
 * 여기서는 target 누락만 {@code ERROR}로 막는다.
 */
@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

	UserListResponse toListResponse(UserSummary summary);

	List<UserListResponse> toListResponses(List<UserSummary> summaries);
}
