package com.ensolution.ems.schedule.infrastructure.excel;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.command.export.SamplingRecordVariable;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
import com.ensolution.ems.schedule.application.port.out.SheetExcelRenderer;
import lombok.extern.slf4j.Slf4j;
import org.jxls.transform.poi.JxlsPoiTemplateFillerBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * jxls-poi 기반 엑셀 렌더러. 업로드된 템플릿에 측정계획 뷰를 채운다.
 * <ul>
 *   <li>채취기록부({@link #renderSamplingRecordsZip}): 시트마다 {@link SamplingRecordVariable}이 정의한 최상위 변수
 *       ({@code plan}·{@code sheet}·측정 영역별 하위 뷰·{@code items}·{@code custom})를 노출해
 *       시트별 파일을 만든 뒤 하나의 ZIP으로 묶는다.</li>
 * </ul>
 * 노출 변수만 컨텍스트에 담아, 표현식이 노출 데이터 밖으로 벗어나지 못하도록 제한한다.
 * <p>
 * 템플릿이 없는 이름을 쓰면 jxls 기본 JEXL(silent·non-strict)이 null로 평가해 <b>빈칸</b>이 된다 — 예외도,
 * 원문 출력도 아니다. 이름 오류는 렌더링이 아니라 템플릿 검사({@code ScheduleExportService#checkTemplate})가 잡는다.
 */
@Slf4j
@Component
public class JxlsSheetExcelRenderer implements SheetExcelRenderer {

	@Override
	public byte[] renderSamplingRecordsZip(byte[] template, ScheduleExportView data) {
		List<SheetExportView> sheets = data.getSheets();
		if (sheets == null || sheets.isEmpty()) {
			log.warn("채취기록부 내보내기 실패: 측정 시트가 없습니다");
			throw new CustomException(ErrorCode.SCHEDULE_EXPORT_FAILED);
		}

		try (ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
		     ZipOutputStream zip = new ZipOutputStream(zipBytes)) {

			for (int i = 0; i < sheets.size(); i++) {
				SheetExportView sheet = sheets.get(i);
				byte[] rendered = fill(template, samplingRecordModel(data, sheet));

				zip.putNextEntry(new ZipEntry(entryName(data.getReferenceNumber(), sheet)));
				zip.write(rendered);
				zip.closeEntry();
			}

			zip.finish();
			return zipBytes.toByteArray();
		} catch (Exception e) {
			log.warn("채취기록부 엑셀 템플릿 렌더링 실패", e);
			throw new CustomException(ErrorCode.SCHEDULE_EXPORT_FAILED);
		}
	}

	/**
	 * 채취기록부 한 장의 컨텍스트. 변수 이름과 값의 출처는 {@link SamplingRecordVariable}이 소유한다 —
	 * 템플릿 검사기가 같은 표를 보므로 여기서 이름을 따로 적지 않는다.
	 */
	private Map<String, Object> samplingRecordModel(ScheduleExportView data, SheetExportView sheet) {
		Map<String, Object> model = new HashMap<>();
		for (SamplingRecordVariable variable : SamplingRecordVariable.values()) {
			model.put(variable.getVariableName(), variable.extract(data, sheet));
		}
		return model;
	}

	/** 단일 템플릿을 주어진 모델로 채워 xlsx 바이트를 반환한다. */
	private byte[] fill(byte[] template, Map<String, Object> model) throws Exception {
		try (InputStream in = new ByteArrayInputStream(template);
		     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			JxlsPoiTemplateFillerBuilder.newInstance()
				.withExceptionThrower()
				// POI가 일부 함수(TYPE 등)를 평가하지 못해 렌더링이 실패하는 문제 회피:
				// 저장 전 POI 수식 평가를 끄고, 대신 엑셀이 열릴 때 재계산되도록 플래그를 설정한다.
				.withRecalculateFormulasBeforeSaving(false)
				.withRecalculateFormulasOnOpening(true)
				.withTemplate(in)
				.build()
				.fill(model, () -> out);

			return out.toByteArray();
		}
	}

	/** ZIP 엔트리명: fKET-A-QP-17-02-01(2) 대기측정기록부({문서번호}) {카테고리}.xlsx */
	private String entryName(String referenceNumber, SheetExportView sheet) {
		String refNum = referenceNumber == null ? "..." : referenceNumber;
		String category = sheet.getCategory();
		
		String suffix = category == null || category.isBlank()
			? ".xlsx"
			: " " + category + ".xlsx";
		
		return "fKET-A-QP-17-02-01(2) 대기측정기록부(" + refNum + ")" + suffix;
	}
}