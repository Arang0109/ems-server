package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;

/**
 * 측정 데이터를 엑셀 템플릿에 채워 렌더링하는 아웃바운드 포트.
 * 구현(jxls/POI)은 인프라 계층에 두고, 애플리케이션은 이 포트로만 엑셀 생성을 요청한다.
 */
public interface SheetExcelRenderer {

	/**
	 * 업로드된 엑셀 템플릿에 측정계획 뷰를 채워 결과 엑셀 바이트를 반환한다(성적서: 전 시트 단일 파일).
	 * @param template 고객사가 업로드한 jxls 문법이 적용된 엑셀 템플릿(xlsx)
	 * @param data     템플릿이 바인딩할 측정계획 뷰
	 */
	byte[] render(byte[] template, ScheduleExportView data);

	/**
	 * 측정 시트별로 템플릿을 채워 각 xlsx를 하나의 ZIP으로 묶어 반환한다(채취기록부: 시트당 파일 1개).
	 * 각 시트 렌더링 시 원장 데이터({@code plan})와 해당 시트({@code sheet})를 함께 바인딩한다.
	 * @param template 고객사가 업로드한 jxls 문법이 적용된 엑셀 템플릿(xlsx). 단일 시트({@code sheet})를 바인딩한다.
	 * @param data     측정계획 뷰. 내부의 시트 목록을 순회해 시트별로 렌더링한다.
	 */
	byte[] renderSamplingRecordsZip(byte[] template, ScheduleExportView data);
}
