package com.ensolution.ems.chat.application.command;

/**
 * 다운로드 응답 페이로드. 파일 바이트와 그것을 내려보내는 데 필요한 헤더 정보를 함께 담는다
 * ({@code storage}의 {@code DocumentFile}과 같은 역할).
 */
public record ChatAttachmentFile(
	String filename,
	String contentType,
	byte[] content
) {
}
