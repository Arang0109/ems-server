package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeCustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.application.command.create.CreateCustomFieldDefinitionCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateCustomFieldDefinitionCommand;
import com.ensolution.ems.schedule.application.validator.CustomFieldDefinitionValidator;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 커스텀 필드 정의 유스케이스의 규칙을 고정한다 — 키는 tenant 안에서만 유일하고, 등록은 목록 맨 뒤에 붙으며,
 * 수정·삭제는 tenant 범위 밖이면 404로 존재를 숨긴다. 키는 어느 경로로도 바뀌지 않는다.
 */
class CustomFieldDefinitionServiceTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	private FakeCustomFieldDefinitionRepository repository;
	private CustomFieldDefinitionService service;

	@BeforeEach
	void setUp() {
		repository = new FakeCustomFieldDefinitionRepository();
		service = new CustomFieldDefinitionService(repository, new CustomFieldDefinitionValidator(repository));
	}

	@Nested
	@DisplayName("등록")
	class Create {

		@Test
		void 같은_고객사에_같은_키가_있으면_거부한다() {
			repository.given(TENANT, "siteCode", "현장 코드", 10);

			assertThatThrownBy(() -> service.createDefinition(
				new CreateCustomFieldDefinitionCommand(TENANT, "siteCode", "다른 라벨", null)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_KEY_ALREADY_EXISTS.getMessage());
		}

		@Test
		void 다른_고객사의_같은_키는_등록된다() {
			repository.given(OTHER_TENANT, "siteCode", "현장 코드", 10);

			CustomFieldDefinition created = service.createDefinition(
				new CreateCustomFieldDefinitionCommand(TENANT, "siteCode", "현장 코드", null));

			assertThat(created.getId()).isNotNull();
			assertThat(created.getTenantId()).isEqualTo(TENANT);
		}

		@Test
		void 표시순서를_비우면_목록_맨_뒤에_붙는다() {
			repository.given(TENANT, "a", "A", 10);
			repository.given(TENANT, "b", "B", 30);
			repository.given(OTHER_TENANT, "c", "C", 100);   // 다른 고객사의 값은 max 계산에 끼지 않는다

			CustomFieldDefinition created = service.createDefinition(
				new CreateCustomFieldDefinitionCommand(TENANT, "d", "D", null));

			assertThat(created.getSortOrder()).isEqualTo(40);
		}

		@Test
		void 표시순서를_주면_그대로_쓴다() {
			CustomFieldDefinition created = service.createDefinition(
				new CreateCustomFieldDefinitionCommand(TENANT, "d", "D", 5));

			assertThat(created.getSortOrder()).isEqualTo(5);
		}

		@Test
		void 키_형식이_틀리면_거부한다() {
			assertThatThrownBy(() -> service.createDefinition(
				new CreateCustomFieldDefinitionCommand(TENANT, "굴뚝명2", "라벨", null)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_INVALID_KEY.getMessage());
		}
	}

	@Nested
	@DisplayName("수정")
	class Update {

		@Test
		void 라벨과_표시순서를_고치고_키는_유지한다() {
			CustomFieldDefinition given = repository.given(TENANT, "siteCode", "현장 코드", 10);

			CustomFieldDefinition updated = service.updateDefinition(given.getId(), TENANT,
				new UpdateCustomFieldDefinitionCommand("현장코드", 20));

			assertThat(updated.getKey()).isEqualTo("siteCode");
			assertThat(updated.getLabel()).isEqualTo("현장코드");
			assertThat(updated.getSortOrder()).isEqualTo(20);
			assertThat(repository.findById(given.getId(), TENANT).getLabel()).isEqualTo("현장코드");
		}

		@Test
		void 다른_고객사의_정의는_404로_숨긴다() {
			CustomFieldDefinition given = repository.given(OTHER_TENANT, "siteCode", "현장 코드", 10);

			assertThatThrownBy(() -> service.updateDefinition(given.getId(), TENANT,
				new UpdateCustomFieldDefinitionCommand("현장코드", null)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_NOT_FOUND.getMessage());
		}
	}

	@Nested
	@DisplayName("삭제")
	class Delete {

		@Test
		void 자기_고객사의_정의를_지운다() {
			CustomFieldDefinition given = repository.given(TENANT, "siteCode", "현장 코드", 10);

			service.deleteDefinition(given.getId(), TENANT);

			assertThat(repository.findAll(TENANT)).isEmpty();
		}

		@Test
		void 다른_고객사의_정의는_404로_숨긴다() {
			CustomFieldDefinition given = repository.given(OTHER_TENANT, "siteCode", "현장 코드", 10);

			assertThatThrownBy(() -> service.deleteDefinition(given.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CUSTOM_FIELD_NOT_FOUND.getMessage());
			assertThat(repository.findAll(OTHER_TENANT)).hasSize(1);
		}
	}

	@Nested
	@DisplayName("목록")
	class ListQuery {

		@Test
		void 자기_고객사의_정의만_표시순서대로_돌려준다() {
			repository.given(TENANT, "b", "B", 20);
			repository.given(TENANT, "a", "A", 10);
			repository.given(OTHER_TENANT, "c", "C", 5);

			List<CustomFieldDefinition> list = service.getDefinitionList(TENANT);

			assertThat(list).extracting(CustomFieldDefinition::getKey).containsExactly("a", "b");
		}
	}
}
