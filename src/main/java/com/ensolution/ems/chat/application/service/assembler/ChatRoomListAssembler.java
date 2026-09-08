package com.ensolution.ems.chat.application.service.assembler;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.chat.application.command.ChatRoomListItem;
import com.ensolution.ems.chat.application.command.ChatRoomPeer;
import com.ensolution.ems.chat.application.command.UnreadCursor;
import com.ensolution.ems.chat.application.port.out.ChatMessageRepository;
import com.ensolution.ems.chat.application.port.out.PresenceStore;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 대화방 목록 한 줄을 조립합니다. 방 자신(MySQL)에는 상대의 이름도 미읽음 수도 없습니다.
 *
 * <h2>조회 횟수를 고정합니다</h2>
 * 방이 몇 개든 <b>이름 조회 1회 + 미읽음 집계 1회 + 프레즌스 조회 1회</b>입니다.
 * <ul>
 *   <li>상대 이름 — {@code getUserList(tenantId)} 한 번으로 테넌트 전원을 받아 Map으로 만듭니다
 *       ({@code client_management}의 {@code TeamAssembler}와 같은 방식). 테넌트당 사용자가 수십 명
 *       규모라 전원을 읽는 편이 벌크 조회 포트를 새로 뚫는 것보다 단순하고, 조회 횟수는 어차피 같습니다.</li>
 *   <li>미읽음 — 방마다 count를 날리면 N+1입니다. 방별 커서를 한꺼번에 넘겨 집계 한 번으로 받습니다.</li>
 *   <li>접속 상태 — 상대들의 id 를 모아 한 번에 묻습니다. 방마다 물으면 여기서 또 N번이 붙습니다.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ChatRoomListAssembler {

	private final UserQueryUseCase userQueryUseCase;
	private final ChatMessageRepository chatMessageRepository;
	private final PresenceStore presenceStore;

	/**
	 * @param myParticipations 내 참가 행들(방 하나당 하나). 읽음 커서가 여기 있다
	 * @param allParticipants  그 방들의 참가자 전원. 상대가 누구인지는 여기서 나온다
	 */
	public List<ChatRoomListItem> assemble(
		Long tenantId,
		Long myUserId,
		List<ChatParticipant> myParticipations,
		List<ChatRoom> rooms,
		List<ChatParticipant> allParticipants
	) {
		Map<Long, UserSummary> userById = userQueryUseCase.getUserList(tenantId).stream()
			.collect(Collectors.toMap(UserSummary::userId, Function.identity(), (a, b) -> a));

		Map<Long, Long> peerIdByRoomId = allParticipants.stream()
			.filter(participant -> !Objects.equals(participant.getUserId(), myUserId))
			.collect(Collectors.toMap(ChatParticipant::getRoomId, ChatParticipant::getUserId, (a, b) -> a));

		Map<Long, ChatRoom> roomById = rooms.stream()
			.collect(Collectors.toMap(ChatRoom::getId, Function.identity()));

		Map<Long, Long> unreadByRoomId = countUnread(tenantId, myUserId, myParticipations);
		// 상대들의 접속 상태도 한 번에 받는다. 방마다 물으면 목록 조회에 N 번이 더 붙는다.
		Set<Long> online = presenceStore.onlineAmong(tenantId, peerIdByRoomId.values());

		return myParticipations.stream()
			.map(participation -> roomById.get(participation.getRoomId()))
			.filter(Objects::nonNull)
			// 최근 대화가 위로. 아직 대화가 없는 방(lastMessageAt == null)은 맨 아래로 보낸다.
			.sorted(Comparator.comparing(ChatRoom::getLastMessageAt,
				Comparator.nullsLast(Comparator.reverseOrder())))
			.map(room -> toListItem(
				room,
				userById.get(peerIdByRoomId.get(room.getId())),
				unreadByRoomId.getOrDefault(room.getId(), 0L),
				online.contains(peerIdByRoomId.get(room.getId()))))
			.toList();
	}

	/** 전역 배지에 쓰는 합계. 방별 수를 그대로 더한다 — 진실의 원천이 하나뿐이어야 배지와 목록이 어긋나지 않는다. */
	public long totalUnread(Long tenantId, Long myUserId, List<ChatParticipant> myParticipations) {
		return countUnread(tenantId, myUserId, myParticipations).values().stream()
			.mapToLong(Long::longValue)
			.sum();
	}

	public ChatRoomPeer peerOf(Long tenantId, Long peerUserId) {
		return toPeer(
			userQueryUseCase.getUser(peerUserId, tenantId),
			presenceStore.onlineAmong(tenantId, List.of(peerUserId)).contains(peerUserId));
	}

	private Map<Long, Long> countUnread(Long tenantId, Long myUserId, List<ChatParticipant> myParticipations) {
		List<UnreadCursor> cursors = myParticipations.stream()
			.map(participation ->
				new UnreadCursor(participation.getRoomId(), participation.getLastReadMessageId()))
			.toList();

		return chatMessageRepository.countUnreadByRoom(tenantId, myUserId, cursors);
	}

	private ChatRoomListItem toListItem(ChatRoom room, UserSummary peer, long unreadCount, boolean online) {
		return new ChatRoomListItem(
			room.getId(),
			toPeer(peer, online),
			room.getLastMessageId(),
			room.getLastMessagePreview(),
			room.getLastMessageAt(),
			unreadCount
		);
	}

	/**
	 * 상대가 조회되지 않는 경우(계정 삭제 등)에도 목록이 통째로 깨지지 않게 빈 정보를 돌려준다.
	 * 대화 기록은 남아 있어야 하기 때문이다.
	 */
	private ChatRoomPeer toPeer(UserSummary peer, boolean online) {
		if (peer == null) {
			return new ChatRoomPeer(null, null, null, false);
		}
		return new ChatRoomPeer(peer.userId(), peer.name(), peer.department(), online);
	}
}
