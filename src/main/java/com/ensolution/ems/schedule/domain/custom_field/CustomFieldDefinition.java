package com.ensolution.ems.schedule.domain.custom_field;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 고객사가 성적서 템플릿에 쓰려고 정의한 <b>커스텀 필드</b>. tenant 소유 애그리거트다.
 *
 * <p>채취기록부 템플릿이 참조할 수 있는 이름은 서버 코드({@code ~ExportView})에 고정돼 있어, 고객이 자기만의
 * 칸(현장 코드·결재선 같은)을 성적서에 넣을 방법이 없었다. 이 애그리거트가 그 이름을 정의하고, 값은 회차마다
 * 스냅샷 문서({@code ScheduleSnapshot.customFields})가 가진다. 템플릿은 {@code ${custom.<key>}}로 읽는다.
 *
 * <p><b>{@code key}는 등록 후 바꿀 수 없다.</b> 키가 곧 고객이 배포한 템플릿의 계약이자 문서에 저장된 맵의
 * 키라서, 바꾸면 템플릿과 저장된 값이 동시에 고아가 된다({@code PollutantCatalog.code}와 같은 판단).
 * 이름을 바꾸려면 삭제 후 다시 등록한다.
 *
 * <p>기본값·필수 여부는 두지 않는다. 기본값을 "언제" 적용할지(회차 생성 시 복사 vs 내보내기 시 폴백)가 곧
 * 사본 문제인데, 이 모듈은 메타/문서 사본을 없애는 방향으로 정리돼 있다. 정의되지 않은 키는 빈칸으로
 * 출력될 뿐이라 필수 여부도 뜻이 없다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CustomFieldDefinition {

	public static final int KEY_MAX_LENGTH = 50;
	public static final int LABEL_MAX_LENGTH = 100;

	/** JEXL이 {@code custom.key}로 해석할 수 있는 식별자. {@code .} 뒤는 ASCII만 허용된다(한글 키 불가). */
	public static final String KEY_REGEX = "^[A-Za-z_][A-Za-z0-9_]*$";
	private static final Pattern KEY_PATTERN = Pattern.compile(KEY_REGEX);

	/**
	 * 키로 쓸 수 없는 이름. JEXL은 Map 키보다 getter를 먼저 찾으므로 {@code custom.empty}는 값이 아니라
	 * {@code Map.isEmpty()}가 찍히고, {@code custom.class}는 클래스명이 찍힌다. 나머지는 JEXL 예약어·연산자다 —
	 * {@code ${custom.eq}}는 파싱 자체가 실패한다.
	 */
	private static final Set<String> RESERVED_KEYS = Set.of(
		"class", "empty", "size",
		"eq", "ne", "lt", "gt", "le", "ge", "and", "or", "not", "null", "true", "false",
		"new", "var", "let", "const", "if", "else", "for", "while", "do", "return", "function"
	);

	private Long id;
	private Long tenantId;
	private String key;
	private String label;
	private Integer sortOrder;

	public static CustomFieldDefinition register(Long tenantId, String key, String label, Integer sortOrder) {
		CustomFieldDefinition definition = CustomFieldDefinition.builder()
			.tenantId(tenantId)
			.key(key == null ? null : key.strip())
			.label(label == null ? null : label.strip())
			.sortOrder(sortOrder)
			.build();
		definition.requireValidKey();
		return definition;
	}

	/** 라벨·표시 순서만 고친다. null·blank는 기존 값을 유지한다. 키는 바꾸지 않는다(클래스 javadoc). */
	public CustomFieldDefinition update(String label, Integer sortOrder) {
		return this.toBuilder()
			.label(keep(label, this.label))
			.sortOrder(sortOrder == null ? this.sortOrder : sortOrder)
			.build();
	}

	/** 키 형식·길이·예약어. 외부 의존이 없는 도메인 불변식이다. */
	public void requireValidKey() {
		if (key == null || key.isBlank()
			|| key.length() > KEY_MAX_LENGTH
			|| !KEY_PATTERN.matcher(key).matches()
			|| RESERVED_KEYS.contains(key)) {
			throw new CustomException(ErrorCode.CUSTOM_FIELD_INVALID_KEY);
		}
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value.strip();
	}
}
