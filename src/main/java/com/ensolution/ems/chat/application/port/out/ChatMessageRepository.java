package com.ensolution.ems.chat.application.port.out;

import com.ensolution.ems.chat.application.command.UnreadCursor;
import com.ensolution.ems.chat.domain.ChatMessage;

import java.util.List;
import java.util.Map;

public interface ChatMessageRepository {

	/** 저장하면서 id를 부여받는다. 그 id가 곧 커서이자 정렬 축이다. */
	ChatMessage save(ChatMessage message);

	ChatMessage findById(String messageId, Long roomId, Long tenantId);

	/**
	 * 최신순 한 페이지. {@code before}가 {@code null}이면 가장 최근부터다.
	 *
	 * @param limit 요청 크기보다 <b>하나 더</b> 읽어 다음 페이지 유무를 판단하는 것은 호출자의 몫이 아니다 —
	 *              여기서는 요청한 만큼만 준다
	 */
	List<ChatMessage> findPage(Long roomId, Long tenantId, String before, int limit);

	/**
	 * 방별 미읽음 수. <b>집계 한 번</b>으로 끝낸다 — 방마다 count를 날리면 목록 조회가 N+1이 된다.
	 * 자기가 보낸 메시지는 세지 않는다.
	 *
	 * @return 미읽음이 있는 방만 담긴다. 0인 방은 키가 없다
	 */
	Map<Long, Long> countUnreadByRoom(Long tenantId, Long readerId, List<UnreadCursor> cursors);
}
