package com.ensolution.ems.storage.application.port.in;

/**
 * 다운로드 응답 페이로드. 파일 바이트와 그것을 내려보내는 데 필요한 헤더 정보를 함께 담는다.
 * {@code filename}은 업로드 당시의 원본 파일명이다.
 */
public record DocumentFile(
	String filename,
	String contentType,
	byte[] content
) {
}
