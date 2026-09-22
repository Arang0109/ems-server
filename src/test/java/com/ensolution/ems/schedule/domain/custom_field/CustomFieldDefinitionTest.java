package com.ensolution.ems.schedule.domain.custom_field;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 커스텀 필드 키의 불변식을 고정한다.
 *
 * <p>키는 곧 템플릿의 {@code ${custom.<key>}} 표기이므로 JEXL이 {@code .} 뒤에서 허용하는 ASCII 식별자여야 하고,
 * Map 키보다 먼저 해석되는 getter 이름({@code empty}·{@code class})과 JEXL 예약어는 값이 아니라 다른 것이
 * 찍히므로 막는다. 그리고 키는 등록 후 바뀌지 않는다.
 */
class CustomFieldDefinitionTest {

	private static final Long TENANT = 1L;

	@Nested
	@DisplayName("키 형식")
	class KeyFormat {

		@ParameterizedTest
		@ValueSource(strings = {"siteCode", "_a1", "STACK_NAME_2", "x"})
		void 영문자_또는_밑줄로_시작하는_ASCII_식별자는_허용한다(String key) {
			assertThatCode(() -> CustomFieldDefinition.register(TENANT, key, "라벨", 10))
				.doesNotThrowAnyException();
		}

		@ParameterizedTest
		@ValueSource(strings = {"굴뚝명2", "site code", "2ndSite", "site.code", "$site", "site-code", ""})
		void 한글_공백_숫자시작_점_달러_하이픈은_거부한다(String key) {
			assertThatThrownBy(() -> CustomFieldDefinition.register(TENANT, key, "라벨", 10))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_INVALID_KEY.getMessage());
		}

		@Test
		void 앞뒤_공백은_잘라내고_받는다() {
			CustomFieldDefinition definition = CustomFieldDefinition.register(TENANT, "  siteCode ", " 현장 코드 ", 10);

			assertThat(definition.getKey()).isEqualTo("siteCode");
			assertThat(definition.getLabel()).isEqualTo("현장 코드");
		}

		@Test
		void 길이_상한을_넘으면_거부한다() {
			String tooLong = "a".repeat(CustomFieldDefinition.KEY_MAX_LENGTH + 1);

			assertThatThrownBy(() -> CustomFieldDefinition.register(TENANT, tooLong, "라벨", 10))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_INVALID_KEY.getMessage());
		}

		@ParameterizedTest
		@ValueSource(strings = {"class", "empty", "size", "eq", "null", "if"})
		void JEXL이_먼저_해석하는_이름과_예약어는_거부한다(String key) {
			assertThatThrownBy(() -> CustomFieldDefinition.register(TENANT, key, "라벨", 10))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_INVALID_KEY.getMessage());
		}
	}

	@Nested
	@DisplayName("수정")
	class Update {

		@Test
		void 라벨과_표시순서만_바뀌고_키는_그대로다() {
			CustomFieldDefinition original = CustomFieldDefinition.register(TENANT, "siteCode", "현장 코드", 10);

			CustomFieldDefinition updated = original.update("현장코드", 20);

			assertThat(updated.getKey()).isEqualTo("siteCode");
			assertThat(updated.getLabel()).isEqualTo("현장코드");
			assertThat(updated.getSortOrder()).isEqualTo(20);
		}

		@Test
		void 비운_값은_기존_값을_유지한다() {
			CustomFieldDefinition original = CustomFieldDefinition.register(TENANT, "siteCode", "현장 코드", 10);

			CustomFieldDefinition updated = original.update("  ", null);

			assertThat(updated.getLabel()).isEqualTo("현장 코드");
			assertThat(updated.getSortOrder()).isEqualTo(10);
		}
	}
}
