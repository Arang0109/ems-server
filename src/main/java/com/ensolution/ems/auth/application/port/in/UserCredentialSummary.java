package com.ensolution.ems.auth.application.port.in;

/**
 * 인증 시점에 필요한 사용자 정보. <b>암호화된 비밀번호를 담는 유일한 공개 VO다.</b>
 * <p>
 * 그래서 일반 조회용 {@link UserSummary}와 나눠 둔다 — 비밀번호가 필요한 곳은 스프링 시큐리티의
 * {@code UserDetailsService} 하나뿐이고, 다른 소비자가 실수로 집어 들 수 있는 자리에 두지 않는다.
 * <p>
 * 테넌트 <b>이름</b>은 여기 없다. 그것은 {@code platform}의 원장이고, auth가 그것을 조회하면
 * {@code platform → auth} 의존과 맞물려 순환이 된다. 두 모듈의 값을 합치는 일은 소비자
 * ({@code global}의 {@code CustomUserDetailsService})가 한다.
 *
 * @param password 암호화된 비밀번호. 평문이 아니다
 * @param role     역할 이름({@code ADMIN} 등). {@code ROLE_} 접두어는 붙어 있지 않다
 */
public record UserCredentialSummary(
	Long userId,
	Long tenantId,
	String username,
	String password,
	String name,
	String role
) {
}
