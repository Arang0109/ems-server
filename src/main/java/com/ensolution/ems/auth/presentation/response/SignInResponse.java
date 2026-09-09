package com.ensolution.ems.auth.presentation.response;

/**
 * 로그인 결과.
 * <p>
 * {@code userId}는 <b>실시간 알림을 해석하는 데 필요합니다.</b> 채팅의 {@code senderId}·
 * {@code readerId}가 모두 숫자 PK라, 클라이언트가 자기 PK를 모르면 내 말풍선과 상대 말풍선을
 * 구분할 수 없습니다. {@code username}으로 대신할 수 없고 액세스 토큰 claim 에도 없습니다.
 */
public record SignInResponse (
	Long userId,
	String accessToken,
	String tenant,
	String username,
	String name,
	Long teamId,
	String teamName,
	String role
) {}
