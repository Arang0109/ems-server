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
  SCHEDULE_NOT_EDITABLE(HttpStatus.CONFLICT, "완료되었거나 취소된 측정계획은 수정할 수 없습니다."),
  SCHEDULE_INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 변경입니다."),
  SCHEDULE_EXPORT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "엑셀 템플릿 처리에 실패했습니다. 템플릿의 jxls 문법을 확인해 주세요."),
  EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장비입니다."),
  EQUIPMENT_INSPECTION_DISABLED(HttpStatus.BAD_REQUEST, "이 장비의 검사 대상이 아닌 검사 종류입니다."),
  ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 권한입니다."),
  TENANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 사업자번호의 고객사가 존재합니다."),
  TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 고객사입니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
  TEAM_MENTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사수입니다."),
  TEAM_MENTEE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부사수입니다."),
  DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문서입니다."),
  DOCUMENT_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문서 버전입니다."),
  DOCUMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 같은 이름의 문서가 존재합니다."),
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