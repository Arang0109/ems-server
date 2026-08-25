package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.mapper.ScheduleExportViewMapper;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 엑셀 템플릿에 채울 내보내기 뷰를 조립한다.
 *
 * <p>성적서 한 장에 필요한 데이터는 두 컬렉션에 나뉘어 있다 — 측정계획 문서
 * ({@code schedule_documents}: 대상·팀·장비·측정항목 스냅샷과 측정 시트)와 실험분석정보
 * ({@code analysis_records}: 실험실 분석값). 컬렉션이 나뉜 것은 실험실 입력이 측정 시트 저장의
 * 문서 단위 낙관적 락과 부딪히지 않게 하려는 의도이며, 그래서 <b>읽는 쪽에서 다시 합쳐야 한다.</b>
 * 그 결합이 이 클래스의 책임이다.
 *
 * <p>조회·결합과 변환을 나눠, 매퍼({@link ScheduleExportViewMapper})는 포트를 모르는
 * 순수 변환으로 유지한다({@link ScheduleSnapshotAssembler}와 같은 성격의 조립 협력자).
 * 호출하는 서비스의 트랜잭션에 참여하므로 {@code @Transactional}을 붙이지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleExportAssembler {

	private final ScheduleDocumentRepository documentRepository;
	private final AnalysisRecordIndexer analysisRecordIndexer;
	private final ScheduleExportViewMapper exportViewMapper;

	/**
	 * 측정계획 문서와 실험분석정보를 읽어 하나의 뷰로 합친다. 두 컬렉션은 {@code pollutantId}로 잇는다.
	 * 문서가 없으면 {@code SCHEDULE_DOCUMENT_NOT_FOUND}가 올라간다(어댑터가 던진다).
	 * 분석 결과는 없어도 정상이다 — 아직 실험실 입력 전인 계획도 성적서 양식은 뽑을 수 있어야 한다.
	 */
	public ScheduleExportView assemble(Long scheduleId, Long tenantId) {
		ScheduleSnapshot snapshot = documentRepository.findByScheduleId(scheduleId, tenantId);
		return exportViewMapper.toExportView(
			snapshot, analysisRecordIndexer.indexByPollutantId(scheduleId, tenantId));
	}
}
