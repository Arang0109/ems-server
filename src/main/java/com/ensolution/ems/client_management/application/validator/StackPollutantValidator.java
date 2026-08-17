package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 시설별 측정물질(StackPollutant) 등록 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class StackPollutantValidator {

	private final StackPollutantRepository stackPollutantRepository;

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
	 *
	 * <p>반드시 측정물질 참조가 <b>pollutantId로 해석된 뒤</b> 호출해야 한다. id로 지정한 항목과
	 * code로 지정한 항목이 같은 물질을 가리키는 경우를 여기서 잡아야 하기 때문이다.
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
