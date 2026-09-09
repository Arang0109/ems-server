package com.ensolution.ems.chat.application.validator;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 대화방 개설 규칙 중 <b>포트 조회가 필요한 것</b>만 담습니다(루트 규칙 10).
 * <p>
 * 상대가 같은 테넌트 소속인지는 이 모듈이 알 수 없고 {@code auth}의 {@code port/in}에 물어야 합니다.
 * 그 조회가 없으면 다른 고객사 사용자의 id를 넣어 대화방을 열 수 있습니다 — 이름·부서가 그대로
 * 노출되고 메시지까지 건너갑니다.
 */
@Component
@RequiredArgsConstructor
public class ChatRoomValidator {

	private final UserQueryUseCase userQueryUseCase;

	/**
	 * 상대가 존재하고 같은 테넌트인지 확인한다.
	 * <p>
	 * 반환값을 쓰지 않는다 — {@code getUser}가 미존재·타 테넌트를 모두 {@code USER_NOT_FOUND}로
	 * 은닉하므로 호출 자체가 검증이다.
	 */
	public void requireSameTenantUser(Long counterpartId, Long tenantId) {
		userQueryUseCase.getUser(counterpartId, tenantId);
	}

	/** 자기 자신과의 대화방은 만들지 않는다. pairKey가 {@code "7:7"}이 되어 개념이 무너진다. */
	public void requireNotSelf(Long requesterId, Long counterpartId) {
		if (Objects.equals(requesterId, counterpartId)) {
			throw new CustomException(ErrorCode.CHAT_SELF_ROOM_NOT_ALLOWED);
		}
	}
}
