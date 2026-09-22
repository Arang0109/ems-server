package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;

import java.util.List;

/**
 * 업로드된 xlsx 템플릿에서 jxls 표현식을 읽어 낸다. POI·JEXL 파싱은 인프라에만 있고, 애플리케이션은
 * 이 목록을 바인딩 계약({@code SamplingRecordVariable}·{@code ~ExportView}·커스텀 필드 정의)과 대조만 한다.
 */
public interface ExcelTemplateReader {

	/**
	 * 모든 시트의 셀 텍스트({@code ${...}})와 셀 메모({@code jx:} 명령)에서 표현식을 뽑아 시트·행·열 순으로 돌려준다.
	 * xlsx가 아니거나 열 수 없으면 {@code SCHEDULE_EXPORT_FAILED}.
	 */
	List<TemplateExpressionRef> readExpressions(byte[] template);
}
