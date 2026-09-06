package com.ensolution.ems.schedule.application.service.assembler;

import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.mapper.ScheduleExportViewMapper;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 엑셀 템플릿에 채울 내보내기 뷰를 조립한다.
 *
 * <p>성적서 한 장에 필요한 데이터는 두 저장소에 나뉘어 있다 — 메타({@code schedules}: 관리번호·
 * 측정분야·측정용도와 채취일자·시료접수일·분석완료일·성적서발행일)와 세부 문서
 * ({@code schedule_documents}: 대상·팀·장비·측정항목 스냅샷과 측정 시트, 실험분석 결과).
 * 성적서 기본정보 표의 값은 메타가 진실이므로 문서에 사본을 두지 않으며, <b>읽는 쪽에서 합친다.</b>
 * 그 결합이 이 클래스의 책임이다.
 *
 * <p>조회·결합과 변환을 나눠, 매퍼({@link ScheduleExportViewMapper})는 포트를 모르는
 * 순수 변환으로 유지한다({@link ScheduleSnapshotAssembler}와 같은 성격의 조립 협력자).
 * 호출하는 서비스의 트랜잭션에 참여하므로 {@code @Transactional}을 붙이지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleExportAssembler {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository documentRepository;
	private final ScheduleExportViewMapper exportViewMapper;

	/**
	 * 메타와 세부 문서를 읽어 하나의 뷰로 합친다. 둘 다 없으면 각각 {@code SCHEDULE_NOT_FOUND},
	 * {@code SCHEDULE_DOCUMENT_NOT_FOUND}가 올라간다(어댑터가 던진다).
	 * 분석 결과는 없어도 정상이다 — 아직 실험실 입력 전인 계획도 성적서 양식은 뽑을 수 있어야 한다.
	 */
	public ScheduleExportView assemble(Long scheduleId, Long tenantId) {
		Schedule meta = scheduleRepository.findById(scheduleId, tenantId);
		ScheduleSnapshot snapshot = documentRepository.findByScheduleId(scheduleId, tenantId);
		return exportViewMapper.toExportView(meta, snapshot);
	}
}
