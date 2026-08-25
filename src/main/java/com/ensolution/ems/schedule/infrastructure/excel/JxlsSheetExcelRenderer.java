package com.ensolution.ems.schedule.infrastructure.excel;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
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
 *   <li>성적서({@link #render}): {@code plan}(측정계획 뷰)과 {@code sheets}(측정 시트 목록 전체),
 *       {@code items}(측정항목 목록)를 노출해 단일 파일을 만든다.</li>
 *   <li>채취기록부({@link #renderSamplingRecordsZip}): 시트마다 {@code plan}(원장 데이터)과 {@code sheet}(해당 시트),
 *       시트의 측정 영역별 하위 뷰, {@code items}(측정항목 목록)를 최상위 변수로 함께 노출해
 *       시트별 파일을 만든 뒤 하나의 ZIP으로 묶는다.</li>
 * </ul>
 * 두 경우 모두 노출 변수만 컨텍스트에 담아, 표현식이 노출 데이터 밖으로 벗어나지 못하도록 제한한다.
 */
@Slf4j
@Component
public class JxlsSheetExcelRenderer implements SheetExcelRenderer {

	@Override
	public byte[] render(byte[] template, ScheduleExportView data) {
		try {
			Map<String, Object> model = new HashMap<>();
			model.put("plan", data);
			model.put("sheets", data.getSheets());
			model.put("items", data.getItems());
			return fill(template, model);
		} catch (Exception e) {
			log.warn("엑셀 템플릿 렌더링 실패", e);
			throw new CustomException(ErrorCode.SCHEDULE_EXPORT_FAILED);
		}
	}

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

				zip.putNextEntry(new ZipEntry(entryName(i + 1, sheet)));
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
	 * 채취기록부 한 장의 컨텍스트. 원장({@code plan})과 해당 시트({@code sheet})에 더해,
	 * 시트의 측정 영역별 하위 뷰와 반복 목록을 최상위 변수로도 노출한다.
	 * 템플릿이 {@code ${sheet.moisture.ratio}} 대신 {@code ${moisture.ratio}}로 짧게 쓸 수 있게 하기 위함이며,
	 * 하위 뷰는 매퍼가 항상 채우므로({@link SheetExportView} 참고) 여기서 null 검사는 필요하지 않다.
	 * <p>
	 * 측정항목({@code items})은 시트가 아니라 계획에 딸린 값이지만, 기록부 서식도 측정항목 칸을 가지므로
	 * 성적서와 같은 이름으로 함께 노출한다.
	 */
	private Map<String, Object> samplingRecordModel(ScheduleExportView data, SheetExportView sheet) {
		Map<String, Object> model = new HashMap<>();
		model.put("plan", data);
		model.put("sheet", sheet);
		model.put("weather", sheet.getWeather());
		model.put("moisture", sheet.getMoisture());
		model.put("gas", sheet.getGas());
		model.put("flow", sheet.getFlow());
		model.put("particle", sheet.getParticle());
		model.put("points", sheet.getPoints());
		model.put("samples", sheet.getSamples());
		model.put("items", data.getItems());
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

	/** ZIP 엔트리명: {순번}_{카테고리}.xlsx (카테고리 null이면 sheet로 대체). */
	private String entryName(int index, SheetExportView sheet) {
		String category = sheet.getCategory() == null || sheet.getCategory().isBlank() ? "sheet" : sheet.getCategory();
		return index + "_" + category + ".xlsx";
	}
}
