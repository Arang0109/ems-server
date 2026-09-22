package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.export.CheckTemplateResult;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.port.out.CustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.application.port.out.ExcelTemplateReader;
import com.ensolution.ems.schedule.application.port.out.SheetExcelRenderer;
import com.ensolution.ems.schedule.application.service.assembler.ScheduleExportAssembler;
import com.ensolution.ems.schedule.application.service.support.UnknownExpressionFinder;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 측정계획 엑셀 내보내기 유스케이스. 저장된 측정 문서와 실험분석정보를 뷰로 평탄화해
 * 업로드된 Jxls 템플릿에 채운다.
 * 측정 데이터 저장·재계산 책임({@link ScheduleService})과 분리해, 내보내기 관심사만 담당한다.
 * 두 컬렉션의 조회·결합은 {@link ScheduleExportAssembler}에 위임한다.
 * 산출물은 한 종류다: 시트별로 나눠 ZIP으로 묶은 채취기록부.
 * <p>
 * 템플릿 검사({@link #checkTemplate})도 여기 둔다 — 렌더러는 없는 이름을 예외 없이 빈칸으로 넘기므로, 고객이 양식을
 * 올리기 전에 이름 오류를 잡을 자리가 따로 필요하다. 알려진 이름은 바인딩 계약과 이 고객사의 커스텀 필드 정의에서 나온다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleExportService {

	private final ScheduleExportAssembler exportAssembler;
	private final SheetExcelRenderer excelRenderer;
	private final ExcelTemplateReader templateReader;
	private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
	private final UnknownExpressionFinder unknownExpressionFinder;

	/** 채취기록부: 측정 시트별로 템플릿을 채워 시트당 파일 하나씩을 하나의 ZIP으로 묶어 반환한다. */
	public byte[] exportSamplingRecords(Long scheduleId, Long tenantId, byte[] template) {
		return excelRenderer.renderSamplingRecordsZip(template, loadView(scheduleId, tenantId));
	}

	/**
	 * 템플릿의 표현식을 바인딩 계약과 대조해 알 수 없는 이름을 돌려준다. 커스텀 키는 호출자 tenant의 정의만 본다.
	 * 문제가 없으면 {@code valid}다. 렌더링은 하지 않는다.
	 */
	public CheckTemplateResult checkTemplate(Long tenantId, byte[] template) {
		List<TemplateExpressionRef> refs = templateReader.readExpressions(template);
		Set<String> customKeys = customFieldDefinitionRepository.findAll(tenantId).stream()
			.map(CustomFieldDefinition::getKey)
			.collect(Collectors.toSet());
		return new CheckTemplateResult(unknownExpressionFinder.find(refs, customKeys));
	}

	private ScheduleExportView loadView(Long scheduleId, Long tenantId) {
		return exportAssembler.assemble(scheduleId, tenantId);
	}
}
