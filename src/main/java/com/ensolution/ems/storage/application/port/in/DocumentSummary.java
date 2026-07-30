package com.ensolution.ems.storage.application.port.in;

import com.ensolution.ems.global.common.enums.DocumentCategory;

import java.time.LocalDateTime;

/** 문서 메타 요약. 목록·단건 조회의 공통 반환 타입이다. */
public record DocumentSummary(
	Long id,
	String name,
	DocumentCategory category,
	String description,
	int latestVersionNo,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {
}
