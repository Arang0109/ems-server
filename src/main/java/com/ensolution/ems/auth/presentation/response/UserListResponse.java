package com.ensolution.ems.auth.presentation.response;

/**
 * 선택지(사수·부사수 배정 등)용 사용자 목록 아이템.
 * <p>
 * 로그인 아이디·이메일·연락처·tenantId를 담지 않는다. 이 응답은 인증된 사용자 전원에게 열려 있고,
 * 그 필드가 필요한 회원 관리 화면은 ADMIN 전용인 {@code /api/admin/members}가 담당한다.
 * 필드를 더할 때는 "권한 없는 사용자에게 보여도 되는 값인가"를 먼저 따진다.
 */
public record UserListResponse(
	Long userId,
	String name,
	String department,
	String role
) {
}
