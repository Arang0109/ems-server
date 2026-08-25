package com.ensolution.ems.global.exception;

import com.ensolution.ems.global.web.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.List;

@Slf4j
@RestControllerAdvice // @RestController 에서 발생한 모든 예외를 가로채서 처리함.
public class GlobalExceptionHandler {
  
  // 로그인 실패 예외처리
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException e) {
    log.warn("[BadCredentialsException] {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("아이디 또는 비밀번호가 일치하지 않습니다."));
  }
  
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex
  ) {
    String message = "요청 본문의 JSON 형식이 올바르지 않습니다.";
    
    // Jackson 파싱 에러 상세 분기 (선택)
    Throwable cause = ex.getCause();
    if (cause instanceof com.fasterxml.jackson.core.JsonParseException jp) {
      message = "JSON 문법 오류: " + jp.getOriginalMessage();
    }
    
    return ResponseEntity
        .badRequest()
        .body(ApiResponse.error(message));
  }
  
  // UNIQUE/FK 충돌 예외처리
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolations(DataIntegrityViolationException e) {
    log.warn("[DataIntegrityViolationException] {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ApiResponse.error("중복된 데이터입니다."));
  }
  
  // 낙관적 락 충돌 예외처리 — 같은 문서를 두 사용자가 동시에 저장한 경우
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
    log.warn("[OptimisticLockingFailureException] {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ApiResponse.error("다른 사용자가 이 데이터를 먼저 저장했습니다. 최신 내용을 불러온 뒤 다시 시도해 주세요."));
  }
  
  // @RequestParam / @PathVariable 검증 예외처리
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolations(ConstraintViolationException e) {
    log.warn("[] {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("잘못된 요청입니다."));
  }
  
  // @Valid 검증 예외처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<List<FieldErrorResponse>>> handleValidation(MethodArgumentNotValidException e) {
    var errors = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(err -> new FieldErrorResponse(
            err.getField(),
            err.getDefaultMessage()
        ))
        .toList();
    
    log.warn("[ValidationError] {}", errors);
    
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("입력값이 올바르지 않습니다.", errors));
  }
  
  // 커스텀 예외 처리
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    if (errorCode.getStatus().is4xxClientError()) {
      log.warn("[CustomException] code: {}, userMessage: {}", errorCode, e.getMessage());
    } else {
      log.error("[CustomException] code: {}, userMessage: {}, cause={}",
          errorCode,
          e.getMessage(),
          e.getCause() != null ? e.getCause().getMessage() : "N/A");
    }
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(ApiResponse.error(e.getMessage()));
  }
  
  // 클라이언트가 먼저 끊은 SSE·비동기 응답. 브라우저 탭을 닫거나 연결이 만료되면 정상적으로 발생한다.
  // 포괄 핸들러에 맡기면 정상 종료가 ERROR 스택으로 남고, 이미 죽은 소켓에 응답 본문을 다시 쓰려 한다.
  // 응답을 쓸 수 없는 상태이므로 ApiResponse 를 반환하지 않는다(규칙 6의 예외).
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleDisconnectedClient(AsyncRequestNotUsableException e) {
    log.debug("[AsyncRequestNotUsableException] 클라이언트 연결이 끊겨 응답을 쓸 수 없습니다: {}", e.getMessage());
  }
  
  // 그 외 모든 예외 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    log.error("[Exception] Unexpected error: ", e);
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("예외가 발생했습니다."));
  }
}