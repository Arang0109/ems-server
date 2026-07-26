package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.mapper.ScheduleExportViewMapper;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.SheetExcelRenderer;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 측정계획 엑셀 내보내기 유스케이스. 저장된 측정 문서를 뷰로 평탄화해 업로드된 jxls 템플릿에 채운다.
 * 측정 데이터 저장·재계산 책임({@link ScheduleService})과 분리해, 내보내기 관심사만 담당한다.
 * 산출물은 두 종류다: 전 시트를 담은 단일 성적서와, 시트별로 나눠 ZIP으로 묶은 채취기록부.
 */
@Service
@RequiredArgsConstructor
public class ScheduleExportService {

	private final ScheduleDocumentRepository documentRepository;
	private final ScheduleExportViewMapper exportViewMapper;
	private final SheetExcelRenderer excelRenderer;

	/** 성적서 발행: 전 시트 데이터를 업로드된 템플릿에 채워 단일 엑셀 바이트를 반환한다. */
	public byte[] exportReport(Long scheduleId, Long tenantId, byte[] template) {
		return excelRenderer.render(template, loadView(scheduleId, tenantId));
	}

	/** 채취기록부: 측정 시트별로 템플릿을 채워 시트당 파일 하나씩을 하나의 ZIP으로 묶어 반환한다. */
	public byte[] exportSamplingRecords(Long scheduleId, Long tenantId, byte[] template) {
		return excelRenderer.renderSamplingRecordsZip(template, loadView(scheduleId, tenantId));
	}

	private ScheduleExportView loadView(Long scheduleId, Long tenantId) {
		ScheduleSnapshot snapshot = documentRepository.findByScheduleId(scheduleId, tenantId);
		return exportViewMapper.toExportView(snapshot);
	}
}
