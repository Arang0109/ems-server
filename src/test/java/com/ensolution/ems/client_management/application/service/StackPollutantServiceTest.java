package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.application.FakeStackPollutantRepository;
import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.service.assembler.PollutantCatalogAssembler;
import com.ensolution.ems.client_management.application.validator.StackPollutantValidator;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 시설별 측정물질 등록 규칙 검증.
 * 등록 대상은 이 고객사가 <b>이미 채택한</b> 측정물질이어야 한다 — 등록 과정에서 물질을 새로 만들지 않는다.
 */
class StackPollutantServiceTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;
	private static final Long STACK = 10L;

	private FakePollutantRepository pollutantRepository;
	private FakePollutantCatalogRepository catalogRepository;
	private FakeStackPollutantRepository stackPollutantRepository;
	private StackPollutantService service;

	@BeforeEach
	void setUp() {
		pollutantRepository = new FakePollutantRepository();
		catalogRepository = new FakePollutantCatalogRepository();
		stackPollutantRepository = new FakeStackPollutantRepository();
		service = new StackPollutantService(
			stackPollutantRepository,
			new StackPollutantValidator(stackPollutantRepository, pollutantRepository),
			new PollutantCatalogAssembler(pollutantRepository, catalogRepository));
	}

	private Long adopt(Long tenantId, String code) {
		PollutantCatalog catalog = catalogRepository.given(code, MeasurementField.AIR, code + "-국문", 200);
		return pollutantRepository.given(tenantId, catalog, null).getId();
	}

	private CreateStackPollutantCommand command(Long stackId, Long pollutantId) {
		return new CreateStackPollutantCommand(
			TENANT, stackId, pollutantId, MeasurementCycle.MONTHLY, null, false);
	}

	@Nested
	@DisplayName("단건 등록")
	class Single {

		@Test
		@DisplayName("채택한 측정물질이면 등록된다")
		void registersAdoptedPollutant() {
			Long pollutantId = adopt(TENANT, "NOX");

			var saved = service.createStackPollutant(command(STACK, pollutantId));

			assertThat(saved.getPollutantId()).isEqualTo(pollutantId);
			assertThat(stackPollutantRepository.saveCount()).isEqualTo(1);
			// 등록이 측정물질을 새로 만들지 않는다
			assertThat(pollutantRepository.saveCount()).isZero();
		}

		@Test
		@DisplayName("존재하지 않는 측정물질이면 등록되지 않는다")
		void rejectsUnknownPollutant() {
			assertThatThrownBy(() -> service.createStackPollutant(command(STACK, 999L)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.NOT_FOUND.getMessage());

			assertThat(stackPollutantRepository.saveCount()).isZero();
		}

		@Test
		@DisplayName("다른 고객사의 측정물질은 등록할 수 없다")
		void rejectsOtherTenantPollutant() {
			Long otherPollutantId = adopt(OTHER_TENANT, "NOX");

			assertThatThrownBy(() -> service.createStackPollutant(command(STACK, otherPollutantId)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.NOT_FOUND.getMessage());

			assertThat(stackPollutantRepository.saveCount()).isZero();
		}

		@Test
		@DisplayName("같은 시설에 같은 물질을 두 번 등록할 수 없다")
		void rejectsDuplicate() {
			Long pollutantId = adopt(TENANT, "NOX");
			service.createStackPollutant(command(STACK, pollutantId));

			assertThatThrownBy(() -> service.createStackPollutant(command(STACK, pollutantId)))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CONFLICT.getMessage());

			assertThat(stackPollutantRepository.saveCount()).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("일괄 등록")
	class Batch {

		@Test
		@DisplayName("모두 채택한 물질이면 한 번에 등록된다")
		void registersAll() {
			List<CreateStackPollutantCommand> commands = List.of(
				command(STACK, adopt(TENANT, "NOX")),
				command(STACK, adopt(TENANT, "SOX")));

			assertThat(service.createStackPollutants(commands)).hasSize(2);
			assertThat(pollutantRepository.saveCount()).isZero();
		}

		@Test
		@DisplayName("소유권 검증은 항목 수와 무관하게 조회 한 번으로 끝낸다")
		void checksOwnershipInOneQuery() {
			List<CreateStackPollutantCommand> commands = List.of(
				command(STACK, adopt(TENANT, "NOX")),
				command(STACK, adopt(TENANT, "SOX")),
				command(STACK, adopt(TENANT, "CO")));

			service.createStackPollutants(commands);

			assertThat(pollutantRepository.listQueryCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("하나라도 남의 물질이면 아무것도 저장하지 않는다")
		void rejectsWholeBatchOnForeignPollutant() {
			List<CreateStackPollutantCommand> commands = List.of(
				command(STACK, adopt(TENANT, "NOX")),
				command(STACK, adopt(OTHER_TENANT, "SOX")));

			assertThatThrownBy(() -> service.createStackPollutants(commands))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.NOT_FOUND.getMessage());

			assertThat(stackPollutantRepository.saveCount()).isZero();
		}

		@Test
		@DisplayName("요청 안에 같은 조합이 겹치면 아무것도 저장하지 않는다")
		void rejectsWholeBatchOnSelfDuplicate() {
			Long pollutantId = adopt(TENANT, "NOX");
			List<CreateStackPollutantCommand> commands =
				List.of(command(STACK, pollutantId), command(STACK, pollutantId));

			assertThatThrownBy(() -> service.createStackPollutants(commands))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CONFLICT.getMessage());

			assertThat(stackPollutantRepository.saveCount()).isZero();
		}

		@Test
		@DisplayName("다른 시설이면 같은 물질을 함께 등록할 수 있다")
		void allowsSamePollutantOnDifferentStacks() {
			Long pollutantId = adopt(TENANT, "NOX");

			assertThat(service.createStackPollutants(
				List.of(command(STACK, pollutantId), command(20L, pollutantId)))).hasSize(2);
		}
	}
}
