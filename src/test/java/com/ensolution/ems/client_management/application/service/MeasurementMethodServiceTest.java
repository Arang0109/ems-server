package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.FakeMeasurementMethodRepository;
import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.application.command.create.CreateMeasurementMethodCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateMeasurementMethodCommand;
import com.ensolution.ems.client_management.application.validator.MeasurementMethodValidator;
import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.SampleGrouping;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정방법 유스케이스의 규칙을 고정한다.
 *
 * <p>핵심은 넷이다 — 이름은 tenant 안에서만 유일하고(수정 시 자기 자신은 제외), 등록은 목록 맨 뒤에 붙으며,
 * 삭제는 소유권(404)을 참조 여부(409)보다 먼저 보고, 기본값 채우기는 이름 기준으로 멱등하다.
 */
class MeasurementMethodServiceTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	private FakeMeasurementMethodRepository methodRepository;
	private FakePollutantRepository pollutantRepository;
	private FakePollutantCatalogRepository catalogRepository;
	private MeasurementMethodService service;

	@BeforeEach
	void setUp() {
		methodRepository = new FakeMeasurementMethodRepository();
		pollutantRepository = new FakePollutantRepository();
		catalogRepository = new FakePollutantCatalogRepository();
		service = new MeasurementMethodService(
			methodRepository,
			new MeasurementMethodValidator(methodRepository, pollutantRepository)
		);
	}

	private static CreateMeasurementMethodCommand create(Long tenantId, String name, SampleGrouping grouping,
	                                                     String mergedSampleName, Integer sortOrder) {
		return new CreateMeasurementMethodCommand(tenantId, name, grouping, mergedSampleName, null, sortOrder);
	}

	@Nested
	@DisplayName("등록")
	class Create {

		@Test
		void 같은_고객사에_같은_이름이_있으면_거부한다() {
			methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null);

			assertThatThrownBy(() -> service.createMeasurementMethod(
				create(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_ALREADY_EXISTS.getMessage());
		}

		@Test
		void 다른_고객사의_같은_이름은_영향을_주지_않는다() {
			methodRepository.given(OTHER_TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null);

			assertThatCode(() -> service.createMeasurementMethod(
				create(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null)))
				.doesNotThrowAnyException();
		}

		@Test
		void 정렬_순서를_주지_않으면_목록_맨_뒤에_붙인다() {
			methodRepository.save(MeasurementMethod.register(TENANT, "먼지", SampleGrouping.NONE, null, null, 10));
			methodRepository.save(MeasurementMethod.register(TENANT, "중금속", SampleGrouping.NONE, null, null, 20));
			methodRepository.save(MeasurementMethod.register(OTHER_TENANT, "남의것", SampleGrouping.NONE, null, null, 500));

			MeasurementMethod created = service.createMeasurementMethod(
				create(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null));

			assertThat(created.getSortOrder()).isEqualTo(30);
		}

		@Test
		void 첫_등록은_10부터_시작한다() {
			MeasurementMethod created = service.createMeasurementMethod(
				create(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null));

			assertThat(created.getSortOrder()).isEqualTo(10);
		}

		@Test
		void 채취_단위와_통칭명이_어긋나면_거부한다() {
			assertThatThrownBy(() -> service.createMeasurementMethod(
				create(TENANT, "카트리지", SampleGrouping.MERGED, null, null)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_GROUPING_MISMATCH.getMessage());
		}
	}

	@Nested
	@DisplayName("수정")
	class Update {

		@Test
		void 자기_이름을_그대로_두고_다른_값만_바꿀_수_있다() {
			Long id = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null).getId();

			MeasurementMethod updated = service.updateMeasurementMethod(
				id, TENANT, new UpdateMeasurementMethodCommand("카트리지", null, "VOCs", 30));

			assertThat(updated.getSamplingMinutes()).isEqualTo(30);
		}

		@Test
		void 다른_측정방법의_이름으로는_바꿀_수_없다() {
			methodRepository.given(TENANT, "흡착관", SampleGrouping.MERGED, "VOCs-T", null);
			Long id = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null).getId();

			assertThatThrownBy(() -> service.updateMeasurementMethod(
				id, TENANT, new UpdateMeasurementMethodCommand("흡착관", null, "VOCs", null)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_ALREADY_EXISTS.getMessage());
		}

		@Test
		void 채취시간은_한_번에_비울_수_있다() {
			Long id = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", 30).getId();

			MeasurementMethod updated = service.updateMeasurementMethod(
				id, TENANT, new UpdateMeasurementMethodCommand(null, null, "VOCs", null));

			assertThat(updated.getSamplingMinutes()).isNull();
		}

		@Test
		void 다른_고객사의_측정방법은_수정할_수_없다() {
			Long id = methodRepository.given(OTHER_TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null).getId();

			assertThatThrownBy(() -> service.updateMeasurementMethod(
				id, TENANT, new UpdateMeasurementMethodCommand(null, null, "VOCs", 30)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND.getMessage());
		}
	}

	@Nested
	@DisplayName("삭제")
	class Delete {

		@Test
		void 측정물질이_쓰고_있으면_삭제할_수_없다() {
			Long id = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null).getId();
			PollutantCatalog hcho = catalogRepository.given("HCHO", MeasurementField.AIR, "포름알데히드", 300);
			pollutantRepository.given(TENANT, hcho, null, id);

			assertThatThrownBy(() -> service.deleteMeasurementMethod(id, TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_IN_USE.getMessage());
		}

		@Test
		void 아무도_쓰지_않으면_삭제된다() {
			Long id = methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null).getId();

			service.deleteMeasurementMethod(id, TENANT);

			assertThat(methodRepository.findAll(TENANT)).isEmpty();
		}

		@Test
		void 다른_고객사의_측정방법은_쓰이고_있어도_존재를_숨기고_NOT_FOUND다() {
			Long id = methodRepository.given(OTHER_TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", null).getId();
			PollutantCatalog hcho = catalogRepository.given("HCHO", MeasurementField.AIR, "포름알데히드", 300);
			pollutantRepository.given(OTHER_TENANT, hcho, null, id);

			assertThatThrownBy(() -> service.deleteMeasurementMethod(id, TENANT))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND.getMessage());
		}
	}

	@Nested
	@DisplayName("기본값 채우기")
	class Defaults {

		@Test
		void 빈_고객사에는_8종이_순서대로_생긴다() {
			List<MeasurementMethod> methods = service.ensureDefaults(TENANT);

			assertThat(methods).extracting(MeasurementMethod::getName)
				.containsExactly("먼지", "중금속", "수은", "현장측정", "흡수액", "흡착관", "테드라백", "카트리지");
			assertThat(methods).filteredOn(m -> m.getName().equals("카트리지"))
				.singleElement()
				.satisfies(m -> {
					assertThat(m.getSampleGrouping()).isEqualTo(SampleGrouping.MERGED);
					assertThat(m.getMergedSampleName()).isEqualTo("VOCs");
					assertThat(m.getSamplingMinutes()).isNull();
				});
		}

		@Test
		void 다시_호출해도_늘어나지_않는다() {
			service.ensureDefaults(TENANT);

			List<MeasurementMethod> methods = service.ensureDefaults(TENANT);

			assertThat(methods).hasSize(8);
		}

		@Test
		void 이미_있는_이름은_고친_값을_되돌리지_않고_부족분만_채운다() {
			methodRepository.given(TENANT, "카트리지", SampleGrouping.MERGED, "알데히드류", 45);

			List<MeasurementMethod> methods = service.ensureDefaults(TENANT);

			assertThat(methods).hasSize(8);
			assertThat(methods).filteredOn(m -> m.getName().equals("카트리지"))
				.singleElement()
				.satisfies(m -> {
					assertThat(m.getMergedSampleName()).isEqualTo("알데히드류");
					assertThat(m.getSamplingMinutes()).isEqualTo(45);
				});
		}

		@Test
		void 다른_고객사에는_영향이_없다() {
			service.ensureDefaults(TENANT);

			assertThat(methodRepository.findAll(OTHER_TENANT)).isEmpty();
		}
	}
}
