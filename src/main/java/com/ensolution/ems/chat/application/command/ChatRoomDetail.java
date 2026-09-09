package com.ensolution.ems.chat.application.command;

/**
 * 대화방 단건. 방을 열었을 때 헤더와 초기 스크롤 위치에 필요한 만큼만 담는다.
 *
 * <p><b>읽음 커서가 둘인 이유</b>는 쓰임이 다르기 때문이다.
 * <ul>
 *   <li>{@code myLastReadMessageId} — 내가 어디까지 읽었나. <b>어디부터 보여 줄까</b>를 정한다</li>
 *   <li>{@code peerLastReadMessageId} — 상대가 어디까지 읽었나. <b>내 말풍선에 "읽음"을 붙일지</b>를 정한다</li>
 * </ul>
 *
 * <p>상대 커서를 실시간 알림({@code chat.reads})으로만 알 수 있게 두면, 상대가 내 접속 전에 읽은
 * 경우를 영영 알 수 없고 새로고침할 때마다 읽음 표시가 사라진다.
 */
public record ChatRoomDetail(
	Long roomId,
	ChatRoomPeer peer,
	String myLastReadMessageId,
	String peerLastReadMessageId
) {
}
