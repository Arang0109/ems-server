package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.application.FakeMeasurementMethodRepository;
import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.SampleGrouping;
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
	private static final Long OTHER_TENANT = 2L;

	private FakePollutantRepository pollutantRepository;
	private FakePollutantCatalogRepository catalogRepository;
	private FakeMeasurementMethodRepository methodRepository;
	private PollutantValidator validator;
	private PollutantCatalogValidator catalogValidator;

	@BeforeEach
	void setUp() {
		pollutantRepository = new FakePollutantRepository();
		catalogRepository = new FakePollutantCatalogRepository();
		methodRepository = new FakeMeasurementMethodRepository();
		validator = new PollutantValidator(pollutantRepository, methodRepository);
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
	@DisplayName("측정방법 소유")
	class MethodOwned {

		@Test
		@DisplayName("이 고객사의 측정방법이면 통과한다")
		void 이_고객사의_측정방법이면_통과한다() {
			Long methodId = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", 30).getId();

			assertThatCode(() -> validator.requireMethodOwned(methodId, TENANT)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("다른 고객사의 측정방법은 존재를 숨기고 NOT_FOUND 다")
		void 다른_고객사의_측정방법은_존재를_숨기고_NOT_FOUND다() {
			Long methodId = methodRepository.given(OTHER_TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", 30).getId();

			assertThatThrownBy(() -> validator.requireMethodOwned(methodId, TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("없는 측정방법도 같은 코드다")
		void 없는_측정방법도_같은_코드다() {
			assertThatThrownBy(() -> validator.requireMethodOwned(999L, TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("수정 경로의 미전달(null)은 검사하지 않는다")
		void 수정_경로의_미전달은_검사하지_않는다() {
			assertThatCode(() -> validator.requireMethodOwned(null, TENANT)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("항목별 채취시간 오버라이드")
	class SamplingMinutesOverride {

		@Test
		void 항목별로_잡는_방법에는_둘_수_있다() {
			Long absorption = methodRepository.given(TENANT, "흡수액", SampleGrouping.PER_ITEM, null, 40).getId();

			assertThatCode(() -> validator.requireSamplingMinutesAllowed(absorption, 60, TENANT)).doesNotThrowAnyException();
		}

		@Test
		void 한_병으로_함께_잡는_방법에는_둘_수_없다() {
			Long cartridge = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", 30).getId();

			assertThatThrownBy(() -> validator.requireSamplingMinutesAllowed(cartridge, 60, TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.POLLUTANT_SAMPLING_MINUTES_NOT_ALLOWED.getMessage());
		}

		@Test
		void 오버라이드를_두지_않으면_방법을_보지_않는다() {
			Long cartridge = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", 30).getId();

			assertThatCode(() -> validator.requireSamplingMinutesAllowed(cartridge, null, TENANT)).doesNotThrowAnyException();
		}

		@Test
		void 측정방법이_없는_레거시_행은_오버라이드를_허용한다() {
			assertThatCode(() -> validator.requireSamplingMinutesAllowed(null, 60, TENANT)).doesNotThrowAnyException();
		}

		@Test
		void 다른_고객사의_방법은_존재를_숨기고_NOT_FOUND다() {
			Long other = methodRepository.given(OTHER_TENANT, "흡수액", SampleGrouping.PER_ITEM, null, 40).getId();

			assertThatThrownBy(() -> validator.requireSamplingMinutesAllowed(other, 60, TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND.getMessage());
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
