package com.ensolution.ems.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 도메인 특화: {DOMAIN}_{CONDITION} 패턴
  SCHEDULE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 사업장·팀·측정일에 등록된 일정이 존재합니다."),
  SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 측정계획입니다."),
  SCHEDULE_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "측정계획 상세 정보를 찾을 수 없습니다."),
  SCHEDULE_NOT_EDITABLE(HttpStatus.CONFLICT, "성적서 작성이 완료되었거나 취소된 측정계획은 수정할 수 없습니다."),
  SCHEDULE_INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 변경입니다."),
  SCHEDULE_NOT_DELETABLE(HttpStatus.CONFLICT, "진행 중인 측정계획은 삭제할 수 없습니다. 취소를 사용해 주세요."),
  SCHEDULE_NOT_REOPENABLE(HttpStatus.CONFLICT, "성적서 작성이 완료되었거나 취소된 측정계획만 재개방할 수 있습니다."),
  SCHEDULE_REOPEN_FORBIDDEN(HttpStatus.FORBIDDEN, "성적서 작성이 완료된 측정계획의 재개방은 관리자만 할 수 있습니다."),
  SCHEDULE_ITEM_NOT_IN_STACK(HttpStatus.BAD_REQUEST, "측정시설에 등록되지 않은 측정항목입니다."),
  SCHEDULE_ITEM_NOT_IN_SCHEDULE(HttpStatus.BAD_REQUEST, "이번 측정계획의 측정항목이 아닙니다."),
  SCHEDULE_ITEM_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "측정항목 목록이 변경되었습니다. 새로고침 후 다시 시도해 주세요."),
  SCHEDULE_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 실험분석정보입니다."),
  SCHEDULE_ANALYSIS_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 측정항목의 실험분석정보가 등록되어 있습니다."),
  SCHEDULE_ANALYSIS_DUPLICATE_ITEM(HttpStatus.BAD_REQUEST, "요청에 같은 측정항목이 두 번 이상 담겨 있습니다."),
  SCHEDULE_SHEET_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 이 측정 데이터를 먼저 저장했습니다. 최신 내용을 불러온 뒤 다시 저장해 주세요."),
  SCHEDULE_EXPORT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "엑셀 템플릿 처리에 실패했습니다. 템플릿의 jxls 문법을 확인해 주세요."),
  EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장비입니다."),
  EQUIPMENT_INSPECTION_DISABLED(HttpStatus.BAD_REQUEST, "이 장비의 검사 대상이 아닌 검사 종류입니다."),
  ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 권한입니다."),
  ROLE_NOT_ASSIGNABLE(HttpStatus.FORBIDDEN, "부여할 수 없는 권한입니다."),
  TENANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 사업자번호의 고객사가 존재합니다."),
  TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 고객사입니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
  REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
  TEAM_MENTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사수입니다."),
  TEAM_MENTEE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부사수입니다."),
  TEAM_MEMBER_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 다른 팀에 배정된 사용자입니다."),
  TEAM_MEMBER_DUPLICATED(HttpStatus.BAD_REQUEST, "사수와 부사수는 같은 사용자일 수 없습니다."),
  POLLUTANT_CATALOG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 측정물질 카탈로그입니다."),
  POLLUTANT_CATALOG_CODE_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 측정물질 코드입니다."),
  POLLUTANT_CATALOG_IN_USE(HttpStatus.CONFLICT, "사용 중인 측정물질 카탈로그는 삭제할 수 없습니다."),
  POLLUTANT_CATALOG_INACTIVE(HttpStatus.BAD_REQUEST, "폐지된 측정물질은 새로 등록할 수 없습니다."),
  POLLUTANT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 등록된 카탈로그 측정물질입니다."),
  FACILITY_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "시설 목록이 변경되었습니다. 새로고침 후 다시 시도해 주세요."),
  PREVENTION_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "시설 목록이 변경되었습니다. 새로고침 후 다시 시도해 주세요."),
  DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문서입니다."),
  DOCUMENT_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문서 버전입니다."),
  DOCUMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 같은 이름의 문서가 존재합니다."),
  DOCUMENT_LAST_VERSION_NOT_DELETABLE(HttpStatus.CONFLICT, "마지막 남은 버전은 삭제할 수 없습니다."),
  DOCUMENT_FILE_EMPTY(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다."),
  DOCUMENT_FILE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "문서의 실제 파일을 찾을 수 없습니다."),
  STORAGE_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다."),
  STORAGE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽지 못했습니다."),

  // 범용 HTTP 상태
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  CONFLICT(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
  VALUE_MUST_NOT_BE_NULL(HttpStatus.BAD_REQUEST, "필수 값이 null 입니다.");

  private final HttpStatus status;
  private final String message;
}