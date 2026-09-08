package com.ensolution.ems.chat.application.command;

/**
 * 응답과 실시간 알림에 싣는 첨부 표시 정보.
 * <p>
 * <b>{@code storageKey}는 담지 않습니다.</b> 클라이언트는 다운로드 엔드포인트를 메시지 id 로 부르며,
 * 보관소 키가 밖으로 나가면 그 자체가 접근 경로가 됩니다.
 */
public record ChatAttachmentInfo(
	String filename,
	String contentType,
	Long size
) {
}
