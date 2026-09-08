package com.ensolution.ems.chat.application.command;

/**
 * 대화를 시작할 수 있는 사람 한 명.
 * <p>
 * {@code email}·{@code tel} 은 담지 않습니다 — 대화 상대를 고르는 데 필요한 정보가 아니고,
 * ADMIN 전용인 {@code /api/admin/members} 가 아니면 노출되지 않던 값입니다.
 */
public record ChatContactListItem(
	Long userId,
	String name,
	String department,
	String role,
	boolean online
) {
}
