package com.ensolution.ems.auth.presentation.response;

/**
 * 선택지(드롭다운) 렌더링용 사용자 목록 항목. <b>전체 인증 사용자</b>에게 나가는 응답이다.
 * <p>
 * <b>{@code username}·{@code email}·{@code tel}·{@code roleId}·{@code tenantId}가 없는 것이 이 타입의 존재 이유다.</b>
 * 측정계획 등록처럼 사람을 고르기만 하면 되는 화면에 전 직원의 로그인 아이디와 연락처를 내려보낼 이유가 없다.
 * 그 필드들이 필요한 관리 화면은 ADMIN 전용인 {@code /api/admin/members}({@code MemberResponse})가 담당한다.
 * <p>
 * 필드를 더하기 전에 "이 값을 같은 테넌트의 모든 사용자가 봐도 되는가"를 먼저 답할 것.
 */
public record UserListResponse(
	Long userId,
	String name,
	String department,
	String role
) {}
