package com.ensolution.ems.chat.application.service.assembler;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.chat.application.command.ChatRoomListItem;
import com.ensolution.ems.chat.application.command.ChatRoomPeer;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 대화방 목록 한 줄을 조립합니다. 방 자신(MySQL)에는 상대의 이름도 접속 상태도 없습니다.
 * <p>
 * <b>조회 횟수를 고정합니다.</b> 상대 이름은 방마다 한 번씩 묻는 대신
 * {@code getUserList(tenantId)} 한 번으로 테넌트 전원을 받아 Map으로 만듭니다
 * ({@code client_management}의 {@code TeamAssembler}가 쓰는 방식). 테넌트당 사용자가
 * 수십 명 규모라 전원을 읽는 편이 벌크 조회 포트를 새로 뚫는 것보다 단순하고, 조회 횟수는 같습니다.
 * <p>
 * 미읽음 수와 접속 상태는 아직 채우지 않습니다 — 각각 메시지 저장소(4단계)와
 * 프레즌스 저장소(7단계)가 들어오면 이 클래스가 받아 조립합니다.
 */
@Component
@RequiredArgsConstructor
public class ChatRoomListAssembler {

	private final UserQueryUseCase userQueryUseCase;

	/**
	 * @param myParticipations 내 참가 행들(방 하나당 하나)
	 * @param rooms            그 방들
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

		return myParticipations.stream()
			.map(participation -> roomById.get(participation.getRoomId()))
			.filter(Objects::nonNull)
			// 최근 대화가 위로. 아직 대화가 없는 방(lastMessageAt == null)은 맨 아래로 보낸다.
			.sorted(Comparator.comparing(ChatRoom::getLastMessageAt,
				Comparator.nullsLast(Comparator.reverseOrder())))
			.map(room -> toListItem(room, userById.get(peerIdByRoomId.get(room.getId()))))
			.toList();
	}

	public ChatRoomPeer peerOf(Long tenantId, Long peerUserId) {
		return toPeer(userQueryUseCase.getUser(peerUserId, tenantId));
	}

	private ChatRoomListItem toListItem(ChatRoom room, UserSummary peer) {
		return new ChatRoomListItem(
			room.getId(),
			toPeer(peer),
			room.getLastMessageId(),
			room.getLastMessagePreview(),
			room.getLastMessageAt(),
			0L
		);
	}

	/**
	 * 상대가 조회되지 않는 경우(계정 삭제 등)에도 목록이 통째로 깨지지 않게 빈 정보를 돌려준다.
	 * 대화 기록은 남아 있어야 하기 때문이다.
	 */
	private ChatRoomPeer toPeer(UserSummary peer) {
		if (peer == null) {
			return new ChatRoomPeer(null, null, null, false);
		}
		return new ChatRoomPeer(peer.userId(), peer.name(), peer.department(), false);
	}
}
