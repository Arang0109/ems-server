package com.ensolution.ems.chat.application.event;

/**
 * 누가 나에게 대화방을 열었다는 알림. 상대가 첫 메시지를 보내기 전에도 목록에 방이 나타나야 한다.
 * <p>
 * 방을 연 사람에게는 보내지 않는다 — 그쪽은 REST 응답으로 이미 방을 받았다.
 */
public record ChatRoomOpenedPayload(
	Long roomId,
	Long peerUserId,
	String peerName,
	String peerDepartment
) {
}
