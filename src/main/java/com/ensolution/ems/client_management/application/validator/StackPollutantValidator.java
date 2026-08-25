package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import com.ensolution.ems.client_management.domain.Pollutant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 시설별 측정물질(StackPollutant) 등록 시의 비즈니스 규칙 검증을 담당한다.
 */
@Component
@RequiredArgsConstructor
public class StackPollutantValidator {

	private final StackPollutantRepository stackPollutantRepository;
	private final PollutantRepository pollutantRepository;

	/**
	 * 등록하려는 측정물질이 이 고객사가 채택해 보유 중인 물질인지 확인한다.
	 * 미존재·타 tenant 모두 {@code NOT_FOUND}로 은닉한다.
	 */
	public void requirePollutantOwned(Long pollutantId, Long tenantId) {
		pollutantRepository.findById(pollutantId, tenantId);
	}

	/**
	 * 일괄 등록 요청의 측정물질이 모두 이 고객사 소유인지 확인한다.
	 * 항목별로 조회하지 않도록 이 tenant의 측정물질을 한 번만 읽어 대조한다.
	 */
	public void requirePollutantsOwned(List<CreateStackPollutantCommand> commands, Long tenantId) {
		Set<Long> owned = pollutantRepository.findAll(tenantId).stream()
			.map(Pollutant::getId)
			.collect(Collectors.toSet());

		for (CreateStackPollutantCommand command : commands) {
			if (!owned.contains(command.pollutantId())) {
				throw new CustomException(ErrorCode.NOT_FOUND);
			}
		}
	}

	/**
	 * 같은 측정시설에 같은 측정물질을 중복 등록할 수 없다.
	 * stackId가 이미 tenant에 종속되므로 tenant 파라미터를 받지 않는다.
	 */
	public void requireNotRegistered(Long stackId, Long pollutantId) {
		if (stackPollutantRepository.existsByStackIdAndPollutantId(stackId, pollutantId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
	}

	/**
	 * 일괄 등록 요청 안에 (측정시설, 측정물질) 조합이 중복으로 들어오지 않았는지 검증한다.
	 * 저장 전에 요청 전체를 먼저 확인해 부분 저장을 막는다.
	 */
	public void requireNoDuplicatesInBatch(List<CreateStackPollutantCommand> commands) {
		Set<String> seen = new HashSet<>();
		for (CreateStackPollutantCommand command : commands) {
			if (!seen.add(command.stackId() + ":" + command.pollutantId())) {
				throw new CustomException(ErrorCode.CONFLICT);
			}
		}
	}
}
