package com.ensolution.ems.chat.application.command;

/**
 * 1:1 대화방 개설 요청.
 *
 * @param tenantId      요청자의 테넌트. 상대도 같은 테넌트여야 한다
 * @param requesterId   대화를 시작하는 사람
 * @param counterpartId 상대
 */
public record OpenDirectRoomCommand(
	Long tenantId,
	Long requesterId,
	Long counterpartId
) {
}
