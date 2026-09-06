package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.function.UnaryOperator;

/**
 * 문서 단위 낙관적 락({@code @Version}) 아래에서 측정계획 스냅샷을 부분 갱신한다.
 *
 * <p>저장 직전에 다른 요청이 같은 문서를 먼저 쓰면 {@link OptimisticLockingFailureException}이
 * 오는데, 이는 "같은 값을 고쳤다"는 뜻이 아니라 두 저장이 <b>물리적으로 겹쳤다</b>는 뜻이다.
 * 그래서 문서를 다시 읽어 변경 함수를 재적용한다. 논리 충돌(같은 기록지를 먼저 저장함)은
 * {@code SheetMerge}의 시트 version 비교가 이미 걸러 예외로 빠져나간다.
 *
 * <p>측정 시트와 실험분석정보가 한 문서에 들어 있으므로 <b>기록지 저장·분석 결과 저장·채취시각
 * 저장·항목 편집이 모두 이 락을 공유한다.</b> 문서를 쓰는 경로는 예외 없이 여기를 지나야 한다 —
 * 어느 경로가 재시도를 타는지 사람이 외우게 두면 나중에 추가되는 경로에서 반드시 빠진다.
 *
 * <p><b>변경 함수의 계약</b> — 지키지 않으면 남의 입력이 조용히 사라진다.
 * <ol>
 *   <li><b>자기 소유 필드만 쓴다.</b> 실험·분석 탭은 {@code items[].analysis}의 실험실 입력 넷,
 *       성적서 탭은 채취시각 둘, 측정 탭은 {@code samplingData.sheets}만 건드린다.
 *       재적용이 남의 입력을 되돌리지 않는 근거가 이것뿐이다.</li>
 *   <li><b>순수해야 한다.</b> 재시도로 여러 번 호출되므로 이벤트 발행·MySQL 저장 같은 부수효과를
 *       넣으면 안 된다. SSE 발행이 커밋 이후로 미뤄져 있고 상태 전이가 이 루프 밖에 있는 이유다.</li>
 *   <li><b>스냅샷과 무관한 검증은 루프 밖에서 끝낸다.</b> {@code requireEditable}이나 요청 안의
 *       중복 검사는 미리 하고, {@code requireItem}처럼 스냅샷이 근거인 검증만 안에서 한다 —
 *       그래야 다시 읽은 문서를 기준으로 판정한다.</li>
 * </ol>
 *
 * <p>호출하는 서비스의 트랜잭션에 참여하므로 {@code @Transactional}을 붙이지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SnapshotWriter {

	/** 물리 충돌 재시도 상한. 호출부와 테스트가 함께 보는 값이라 공개한다. */
	public static final int MAX_ATTEMPTS = 3;

	private final ScheduleDocumentRepository scheduleDocumentRepository;

	/**
	 * 문서를 읽어 변경 함수를 적용하고 저장한다. 물리 충돌이면 다시 읽어 재시도하며,
	 * 끝내 실패하면 사용자에게 알린다.
	 */
	public ScheduleSnapshot write(Long scheduleId, Long tenantId, UnaryOperator<ScheduleSnapshot> mutation) {
		int attempt = 1;
		
		while (attempt <= MAX_ATTEMPTS) {
			ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(scheduleId, tenantId);
			
			try {
				return scheduleDocumentRepository.save(mutation.apply(snapshot));
			} catch (OptimisticLockingFailureException e) {
				if (attempt == MAX_ATTEMPTS) {
					throw new CustomException(ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT,
						ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT.getMessage(), e);
				}
			}
			
			attempt++;
		}
		
		throw new CustomException(ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT);
	}
}
