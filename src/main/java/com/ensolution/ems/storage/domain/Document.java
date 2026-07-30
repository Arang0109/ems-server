package com.ensolution.ems.storage.domain;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 버전 이력을 갖는 논리적 문서. 실제 파일 바이트는 {@link DocumentVersion}이 보관하며,
 * 이 애그리거트는 문서의 메타(이름·분류·설명)와 최신 버전 번호만 관리한다.
 */
@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Document {

	private Long id;
	private Long tenantId;

	private String name;
	private DocumentCategory category;
	private String description;

	/** 마지막으로 등록된 버전 번호. 문서 생성 직후에는 0이고, 첫 버전이 저장되면 1이 된다. */
	private int latestVersionNo;

	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Document register(Long tenantId, String name, DocumentCategory category, String description) {
		return Document.builder()
			.tenantId(tenantId)
			.name(name)
			.category(category)
			.description(description)
			.latestVersionNo(0)
			.build();
	}

	public Document update(String name, DocumentCategory category, String description) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.category(category == null ? this.category : category)
			.description(keep(description, this.description))
			.build();
	}

	/** 다음에 등록될 버전 번호. */
	public int nextVersionNo() {
		return this.latestVersionNo + 1;
	}

	/** 새 버전이 등록된 뒤 최신 버전 번호를 올린 사본을 반환한다. */
	public Document withNextVersion() {
		return this.toBuilder()
			.latestVersionNo(nextVersionNo())
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
