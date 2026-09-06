package com.ensolution.ems.contract.application.service;

import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.FakeContractRepository;
import com.ensolution.ems.contract.application.service.assembler.ContractDetailAssembler;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.application.mapper.ContractSummaryMapperImpl;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.domain.ContractAmountUnit;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 계약의 tenant 소유권 격리를 고정한다.
 *
 * <p>계약은 <b>사업장이라는 부모를 타 모듈({@code client_management})에 두고 있어</b> 격리가 두 겹이다 —
 * 계약 자신의 tenant 범위와, 생성 시 지정하는 사업장이 호출자 tenant 소속인지. 둘 중 하나만 지켜도
 * 교차 테넌트가 열린다: 사업장 확인을 빠뜨리면 <b>남의 사업장에 내 계약을 매다는</b> 경로가 생긴다.
 *
 * <p>소유권 불일치를 403이 아니라 {@code NOT_FOUND}로 은닉한다는 규칙 13의 요구도 함께 고정한다.
 */
class ContractServiceTest {

	private static final long TENANT = 1L;
	private static final long OTHER_TENANT = 2L;

	private static final long WORKPLACE = 10L;
	private static final long OTHER_TENANT_WORKPLACE = 20L;

	private final FakeContractRepository contractRepository = new FakeContractRepository();
	private final FakeWorkplaceQuery workplaceQuery = new FakeWorkplaceQuery();

	private final ContractService contractService = new ContractService(
		contractRepository,
		workplaceQuery,
		new ContractDetailAssembler(workplaceQuery),
		new ContractSummaryMapperImpl()
	);

	ContractServiceTest() {
		workplaceQuery.given(WORKPLACE, TENANT);
		workplaceQuery.given(OTHER_TENANT_WORKPLACE, OTHER_TENANT);
	}

	private static CreateContractCommand createCommand(Long tenantId, Long workplaceId) {
		return new CreateContractCommand(
			tenantId, workplaceId, "정기 측정 계약",
			LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31),
			BigDecimal.valueOf(1000), ContractAmountUnit.ANNUAL, true,
			BigDecimal.ZERO, BigDecimal.ZERO, 30, 3, null);
	}

	@Nested
	@DisplayName("생성 — 부모 사업장의 소유권")
	class Create {

		@Test
		@DisplayName("다른 테넌트의 사업장에는 계약을 만들 수 없다")
		void 남의_사업장에는_만들_수_없다() {
			assertThatThrownBy(() -> contractService.createContract(
				createCommand(TENANT, OTHER_TENANT_WORKPLACE)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

			assertThat(contractRepository.count()).isZero();
		}

		@Test
		@DisplayName("존재하지 않는 사업장에도 만들 수 없다 — 타 tenant와 같은 응답으로 존재를 숨긴다")
		void 없는_사업장에는_만들_수_없다() {
			assertThatThrownBy(() -> contractService.createContract(createCommand(TENANT, 404L)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
		}

		@Test
		@DisplayName("자기 테넌트의 사업장이면 생성된다")
		void 자기_사업장에는_만들_수_있다() {
			contractService.createContract(createCommand(TENANT, WORKPLACE));

			assertThat(contractRepository.count()).isEqualTo(1);
			assertThat(contractRepository.findAllByTenantId(TENANT))
				.extracting(ContractListItem::workplaceId)
				.containsExactly(WORKPLACE);
		}
	}

	@Nested
	@DisplayName("조회·수정·삭제 — 계약 자신의 tenant 범위")
	class TenantIsolation {

		@Test
		@DisplayName("다른 테넌트의 계약은 조회할 수 없다")
		void 남의_계약은_조회할_수_없다() {
			Contract stranger = contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약");

			assertThatThrownBy(() -> contractService.getContract(stranger.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
		}

		@Test
		@DisplayName("다른 테넌트의 계약은 수정할 수 없다")
		void 남의_계약은_수정할_수_없다() {
			Contract stranger = contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약");

			assertThatThrownBy(() -> contractService.updateContract(stranger.getId(), TENANT,
				new UpdateContractCommand("바뀐 이름", null, null, null, null, null, null, null, null, null, null, null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

			assertThat(contractRepository.peek(stranger.getId()).orElseThrow().getContractName())
				.isEqualTo("남의 계약");
		}

		@Test
		@DisplayName("다른 테넌트의 계약은 삭제할 수 없다")
		void 남의_계약은_삭제할_수_없다() {
			Contract stranger = contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약");

			assertThatThrownBy(() -> contractService.deleteContract(stranger.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

			assertThat(contractRepository.peek(stranger.getId())).isPresent();
		}
	}

	@Nested
	@DisplayName("목록 — 부모 id를 파라미터로 받는 경로")
	class ListQuery {

		@Test
		@DisplayName("다른 테넌트의 사업장 id로 목록을 조회해도 비어 있다")
		void 남의_사업장_목록은_비어있다() {
			contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약");

			assertThat(contractService.getContractList(OTHER_TENANT_WORKPLACE, TENANT)).isEmpty();
		}

		@Test
		@DisplayName("전체 목록에도 다른 테넌트의 계약이 섞이지 않는다")
		void 전체_목록에_남의_계약이_없다() {
			contractRepository.given(TENANT, WORKPLACE, "내 계약");
			contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약");

			assertThat(contractService.getContractList(null, TENANT))
				.extracting(ContractListItem::contractName)
				.containsExactly("내 계약");
		}

		@Test
		@DisplayName("통계도 자기 테넌트만 센다")
		void 통계는_자기_테넌트만_센다() {
			contractRepository.given(TENANT, WORKPLACE, "내 계약");
			contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약1");
			contractRepository.given(OTHER_TENANT, OTHER_TENANT_WORKPLACE, "남의 계약2");

			assertThat(contractService.countContracts(TENANT)).isEqualTo(1);
		}
	}

	/**
	 * tenant 범위 존재 확인을 재현하는 {@link WorkplaceQueryUseCase}.
	 * 타 tenant의 사업장은 <b>존재하지 않는 것으로</b> 답한다 — 포트 javadoc이 선언한 규약이다.
	 */
	private static class FakeWorkplaceQuery implements WorkplaceQueryUseCase {

		private final java.util.Map<Long, Long> tenantByWorkplaceId = new java.util.HashMap<>();

		void given(Long workplaceId, Long tenantId) {
			tenantByWorkplaceId.put(workplaceId, tenantId);
		}

		@Override
		public boolean existsById(Long workplaceId, Long tenantId) {
			return Objects.equals(tenantId, tenantByWorkplaceId.get(workplaceId));
		}

		@Override
		public ContractSummary getSummaryById(Long workplaceId, Long tenantId) {
			if (!existsById(workplaceId, tenantId)) {
				throw new CustomException(ErrorCode.NOT_FOUND);
			}
			return new ContractSummary("의뢰기관", "사업장", "주소");
		}

		@Override
		public long countWorkplaces(Long tenantId) {
			throw new UnsupportedOperationException();
		}
	}

}
