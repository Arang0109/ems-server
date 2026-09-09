package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.service.assembler.ScheduleExportAssembler;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.port.out.SheetExcelRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 측정계획 엑셀 내보내기 유스케이스. 저장된 측정 문서와 실험분석정보를 뷰로 평탄화해
 * 업로드된 Jxls 템플릿에 채운다.
 * 측정 데이터 저장·재계산 책임({@link ScheduleService})과 분리해, 내보내기 관심사만 담당한다.
 * 두 컬렉션의 조회·결합은 {@link ScheduleExportAssembler}에 위임한다.
 * 산출물은 한 종류다: 시트별로 나눠 ZIP으로 묶은 채취기록부.
 */
@Service
@RequiredArgsConstructor
public class ScheduleExportService {

	private final ScheduleExportAssembler exportAssembler;
	private final SheetExcelRenderer excelRenderer;

	/** 채취기록부: 측정 시트별로 템플릿을 채워 시트당 파일 하나씩을 하나의 ZIP으로 묶어 반환한다. */
	public byte[] exportSamplingRecords(Long scheduleId, Long tenantId, byte[] template) {
		return excelRenderer.renderSamplingRecordsZip(template, loadView(scheduleId, tenantId));
	}

	private ScheduleExportView loadView(Long scheduleId, Long tenantId) {
		return exportAssembler.assemble(scheduleId, tenantId);
	}
}
