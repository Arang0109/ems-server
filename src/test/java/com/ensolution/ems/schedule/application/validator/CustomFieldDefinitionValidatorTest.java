package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeCustomFieldDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 커스텀 필드 정의 검증 규칙을 고정한다 — 키 유일성은 tenant 안에서만이고, 회차 값의 키는 전부 정의돼
 * 있어야 하며 어느 키가 문제인지 메시지로 알려준다.
 */
class CustomFieldDefinitionValidatorTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	private FakeCustomFieldDefinitionRepository repository;
	private CustomFieldDefinitionValidator validator;

	@BeforeEach
	void setUp() {
		repository = new FakeCustomFieldDefinitionRepository();
		validator = new CustomFieldDefinitionValidator(repository);
	}

	@Nested
	@DisplayName("requireUniqueKey")
	class UniqueKey {

		@Test
		void 같은_고객사에_같은_키가_있으면_거부한다() {
			repository.given(TENANT, "siteCode", "현장 코드", 10);

			assertThatThrownBy(() -> validator.requireUniqueKey("siteCode", TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_KEY_ALREADY_EXISTS.getMessage());
		}

		@Test
		void 다른_고객사의_같은_키는_통과한다() {
			repository.given(OTHER_TENANT, "siteCode", "현장 코드", 10);

			assertThatCode(() -> validator.requireUniqueKey("siteCode", TENANT))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("requireDefinedKeys")
	class DefinedKeys {

		@Test
		void 정의된_키만_있으면_통과한다() {
			repository.given(TENANT, "siteCode", "현장 코드", 10);
			repository.given(TENANT, "inspector", "점검자", 20);

			assertThatCode(() -> validator.requireDefinedKeys(Set.of("siteCode", "inspector"), TENANT))
				.doesNotThrowAnyException();
		}

		@Test
		void 빈_집합은_통과한다() {
			assertThatCode(() -> validator.requireDefinedKeys(Set.of(), TENANT))
				.doesNotThrowAnyException();
		}

		@Test
		void 미정의_키가_있으면_그_키를_메시지에_실어_거부한다() {
			repository.given(TENANT, "siteCode", "현장 코드", 10);

			assertThatThrownBy(() -> validator.requireDefinedKeys(Set.of("siteCode", "nope", "alsoNope"), TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(ErrorCode.CUSTOM_FIELD_NOT_DEFINED.getMessage())
				.hasMessageContaining("alsoNope, nope");
		}

		@Test
		void 다른_고객사의_정의는_보지_않는다() {
			repository.given(OTHER_TENANT, "siteCode", "현장 코드", 10);

			assertThatThrownBy(() -> validator.requireDefinedKeys(Set.of("siteCode"), TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining("siteCode");
		}
	}
}
