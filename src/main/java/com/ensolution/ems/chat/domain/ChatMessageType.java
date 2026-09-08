package com.ensolution.ems.chat.domain;

/**
 * 메시지의 종류. 클라이언트가 말풍선을 어떻게 그릴지 가른다.
 * <p>
 * {@code IMAGE}와 {@code FILE}의 구분은 업로드된 {@code contentType}이 정한다 —
 * 사용자가 고르는 값이 아니다.
 */
public enum ChatMessageType {

	TEXT,
	IMAGE,
	FILE
}
