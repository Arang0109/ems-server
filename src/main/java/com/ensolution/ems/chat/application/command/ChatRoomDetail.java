package com.ensolution.ems.chat.application.command;

/** 대화방 단건. 방을 열었을 때 헤더와 초기 스크롤 위치에 필요한 만큼만 담는다. */
public record ChatRoomDetail(
	Long roomId,
	ChatRoomPeer peer,
	String lastReadMessageId
) {
}
