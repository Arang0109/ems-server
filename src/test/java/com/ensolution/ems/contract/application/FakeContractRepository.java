package com.ensolution.ems.contract.application;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.port.out.ContractRepository;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 계약 tenant 격리 검증용 인메모리 {@link ContractRepository}.
 * <p>
 * <b>tenant 필터를 실제 어댑터의 WHERE 절 그대로 재현한다.</b> 이 Fake가 tenant를 무시하면
 * 격리 테스트가 통과해 버려 검증이 무의미해진다. 미존재·타 tenant를 구분하지 않고 똑같이
 * {@code NOT_FOUND}를 던지는 것도 어댑터와 같다(리소스 존재 은닉).
 */
public class FakeContractRepository implements ContractRepository {

	private final List<Contract> contracts = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	public Contract given(Long tenantId, Long workplaceId, String contractName) {
		Contract contract = Contract.builder()
			.id(sequence.incrementAndGet())
			.tenantId(tenantId)
			.workplaceId(workplaceId)
			.contractName(contractName)
			.build();
		contracts.add(contract);
		return contract;
	}

	public Optional<Contract> peek(Long id) {
		return contracts.stream().filter(c -> Objects.equals(id, c.getId())).findFirst();
	}

	public int count() {
		return contracts.size();
	}

	@Override
	public Contract save(Contract contract) {
		contracts.removeIf(stored -> contract.getId() != null && Objects.equals(stored.getId(), contract.getId()));

		Contract saved = contract.getId() == null
			? contract.toBuilder().id(sequence.incrementAndGet()).build()
			: contract;
		contracts.add(saved);
		return saved;
	}

	@Override
	public Contract findById(Long id, Long tenantId) {
		return contracts.stream()
			.filter(c -> Objects.equals(id, c.getId()))
			.filter(c -> Objects.equals(tenantId, c.getTenantId()))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<ContractListItem> findByWorkplaceId(Long workplaceId, Long tenantId) {
		return contracts.stream()
			.filter(c -> Objects.equals(workplaceId, c.getWorkplaceId()))
			.filter(c -> Objects.equals(tenantId, c.getTenantId()))
			.sorted(Comparator.comparing(Contract::getId))
			.map(FakeContractRepository::toListItem)
			.toList();
	}

	@Override
	public List<ContractListItem> findAllByTenantId(Long tenantId) {
		return contracts.stream()
			.filter(c -> Objects.equals(tenantId, c.getTenantId()))
			.sorted(Comparator.comparing(Contract::getId))
			.map(FakeContractRepository::toListItem)
			.toList();
	}

	/** 삭제된 행이 없으면 어댑터와 동일하게 NOT_FOUND. */
	@Override
	public void deleteById(Long id, Long tenantId) {
		boolean removed = contracts.removeIf(c ->
			Objects.equals(id, c.getId()) && Objects.equals(tenantId, c.getTenantId()));
		if (!removed) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}

	/** 사업장 삭제 이벤트를 받는 경로다. 부모가 이미 tenant 확인을 마쳤으므로 tenant를 받지 않는다. */
	@Override
	public void deleteByWorkplaceId(Long workplaceId) {
		contracts.removeIf(c -> Objects.equals(workplaceId, c.getWorkplaceId()));
	}

	private static ContractListItem toListItem(Contract contract) {
		return new ContractListItem(
			contract.getId(),
			contract.getWorkplaceId(),
			contract.getContractName(),
			null, null,
			contract.getContractDate(),
			contract.getStartDate(),
			contract.getCompletionDate(),
			null
		);
	}
}
