package com.ensolution.ems.chat.application.command;

/**
 * 업로드된 파일 한 건. presentation 계층이 {@code MultipartFile}을 이것으로 바꿔 전달하므로
 * Spring Web 타입이 application·domain 으로 새지 않는다({@code storage}의 {@code UploadedFile}과 같은 역할).
 */
public record AttachmentUpload(
	String originalFilename,
	String contentType,
	Long size,
	byte[] content
) {
}
