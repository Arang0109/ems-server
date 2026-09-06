package com.ensolution.ems.auth.application.port.in;

import java.util.Optional;

/**
 * 인증 주체 조회. <b>{@code global}의 {@code UserDetailsService} 전용 계약이다.</b>
 * <p>
 * 비밀번호를 노출하므로 일반 조회 계약({@link UserQueryUseCase})과 분리했다. 새 소비자를 붙이기 전에
 * 정말 비밀번호가 필요한지 먼저 확인하고, 아니면 {@code UserQueryUseCase}를 쓴다.
 * <p>
 * 이 포트가 생긴 이유는 {@code global}이 auth의 JPA 엔티티·리포지토리를 직접 참조하고 있었기 때문이다
 * (루트 {@code CLAUDE.md} 규칙 1·4 위반). 인증은 요청마다 도는 경로라 계층을 한 겹 더 두는 것이
 * 망설여졌지만, <b>모듈 경계를 관통하는 유일한 지점</b>으로 남겨 두는 비용이 더 컸다.
 */
public interface UserCredentialQueryUseCase {

	/**
	 * 로그인 아이디로 인증 주체를 찾는다. 계정이 없으면 빈 값이다 —
	 * 없는 계정은 예외가 아니라 인증 실패이며, 그 판단은 호출자(스프링 시큐리티)가 한다.
	 */
	Optional<UserCredentialSummary> findCredentialByUsername(String username);
}
