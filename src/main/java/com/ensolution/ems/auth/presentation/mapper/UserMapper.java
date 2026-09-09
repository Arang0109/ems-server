package com.ensolution.ems.auth.presentation.mapper;

import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.auth.presentation.response.UserListResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * auth 자기 응답 DTO 변환.
 * <p>
 * {@link UserSummary}는 타 모듈 공개 계약이지만 자기 모듈 presentation도 같은 VO를 쓴다 —
 * {@code UserService}가 이미 역할 이름을 붙여 돌려주므로 조회 계약을 하나 더 둘 이유가 없다.
 * <p>
 * {@code unmappedTargetPolicy}를 ERROR로 두지 않은 것은 의도다. {@link UserListResponse}는
 * {@code UserSummary}의 <b>부분집합</b>이라 source 쪽 필드가 남는 것이 정상이며,
 * auth가 공개 VO에 필드를 더해도 이 응답이 자동으로 넓어져서는 안 된다.
 */
@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface UserMapper {
	List<UserListResponse> toListResponses(List<UserSummary> summaries);
}
