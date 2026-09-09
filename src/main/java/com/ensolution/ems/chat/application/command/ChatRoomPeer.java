package com.ensolution.ems.chat.application.command;

/**
 * 대화 상대의 표시 정보. 이름·부서는 {@code auth}의 {@code UserQueryUseCase}에서,
 * {@code online}은 프레즌스 저장소에서 옵니다 — 이 모듈이 소유한 값이 아니라 조립한 값입니다.
 * <p>
 * {@code email}·{@code tel}은 담지 않습니다. 대화 상대를 고르는 데 필요한 정보가 아니고,
 * ADMIN 전용인 {@code /api/admin/members}가 아니면 노출되지 않던 값입니다.
 */
public record ChatRoomPeer(
	Long userId,
	String name,
	String department,
	boolean online
) {
}
