package com.ensolution.ems.storage.application.port.in;

/**
 * 업로드된 파일 한 건의 페이로드.
 * <p>
 * presentation 계층이 {@code MultipartFile}을 이 타입으로 바꿔 전달한다.
 * 덕분에 Spring Web 타입이 application·domain 계층으로 새지 않는다.
 */
public record UploadedFile(
	String originalFilename,
	String contentType,
	Long size,
	byte[] content
) {
}
