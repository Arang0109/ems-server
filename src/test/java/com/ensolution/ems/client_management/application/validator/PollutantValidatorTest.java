package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PollutantValidatorTest {

	private static final Long TENANT = 1L;

	private FakePollutantRepository pollutantRepository;
	private FakePollutantCatalogRepository catalogRepository;
	private PollutantValidator validator;
	private PollutantCatalogValidator catalogValidator;

	@BeforeEach
	void setUp() {
		pollutantRepository = new FakePollutantRepository();
		catalogRepository = new FakePollutantCatalogRepository();
		validator = new PollutantValidator(pollutantRepository);
		catalogValidator = new PollutantCatalogValidator(catalogRepository, pollutantRepository);
	}

	@Nested
	@DisplayName("채택 가능 여부")
	class Selectable {

		@Test
		@DisplayName("사용 중인 가이드 항목은 채택할 수 있다")
		void allowsActiveCatalog() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);

			assertThatCode(() -> validator.requireSelectable(nox)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("폐지된 가이드 항목은 새로 채택할 수 없다")
		void rejectsInactiveCatalog() {
			PollutantCatalog retired =
				catalogRepository.given("PCE", MeasurementField.AIR, "테트라클로로에틸렌", 460, false);

			assertThatThrownBy(() -> validator.requireSelectable(retired))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.POLLUTANT_CATALOG_INACTIVE.getMessage());
		}
	}

	@Nested
	@DisplayName("중복 채택 방지")
	class CatalogLink {

		@Test
		@DisplayName("같은 가이드 항목을 두 번 채택하면 거부한다")
		void rejectsDuplicateLink() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(TENANT, nox, null);

			assertThatThrownBy(() -> validator.requireCatalogNotLinked(nox.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.POLLUTANT_ALREADY_LINKED.getMessage());
		}

		@Test
		@DisplayName("다른 고객사가 채택한 것은 영향을 주지 않는다")
		void allowsSameCatalogForOtherTenant() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(2L, nox, null);

			assertThatCode(() -> validator.requireCatalogNotLinked(nox.getId(), TENANT))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("카탈로그 규칙")
	class Catalog {

		@Test
		@DisplayName("같은 측정분야에서 code가 중복되면 거부한다")
		void rejectsDuplicateCodeInSameField() {
			catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);

			assertThatThrownBy(() -> catalogValidator.requireUniqueCode(MeasurementField.AIR, "NOX"))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.POLLUTANT_CATALOG_CODE_DUPLICATED.getMessage());
		}

		@Test
		@DisplayName("측정분야가 다르면 같은 code를 쓸 수 있다")
		void allowsSameCodeInOtherField() {
			catalogRepository.given("PB", MeasurementField.AIR, "납", 500);

			assertThatCode(() -> catalogValidator.requireUniqueCode(MeasurementField.WATER, "PB"))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("어느 고객사든 쓰고 있으면 삭제를 막는다")
		void rejectsDeleteWhenReferenced() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(2L, nox, null);

			assertThatThrownBy(() -> catalogValidator.requireNotReferenced(nox.getId()))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.POLLUTANT_CATALOG_IN_USE.getMessage());
		}

		@Test
		@DisplayName("아무도 쓰지 않으면 삭제할 수 있다")
		void allowsDeleteWhenUnused() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);

			assertThatCode(() -> catalogValidator.requireNotReferenced(nox.getId())).doesNotThrowAnyException();
		}
	}
}
