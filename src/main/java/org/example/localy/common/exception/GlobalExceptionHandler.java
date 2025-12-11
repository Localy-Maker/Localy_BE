package org.example.localy.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.errorCode.GlobalErrorCode;
import org.example.localy.common.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 커스텀 예외
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException ex) {
    log.error("Custom 오류 발생: {}", ex.getMessage());
    return ResponseEntity
        .status(ex.getErrorCode().getStatus())
        .body(BaseResponse.failure(
            ex.getErrorCode().getCode(),       // 예: "E001"
            ex.getErrorCode().getMessage()     // 예: "임시 주소 저장 실패"
        ));
  }

  // Validation 실패
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Object>> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errorMessages =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> String.format("[%s] %s", e.getField(), e.getDefaultMessage()))
            .collect(Collectors.joining(" / "));

    log.warn("Validation 오류 발생: {}", errorMessages);

    return ResponseEntity
        .badRequest()
        .body(BaseResponse.failure("VALIDATION_ERROR", errorMessages));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<BaseResponse<Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
    log.warn("Resource Not Found: {}", ex.getMessage());

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND) // 404 상태 코드 반환
            .body(BaseResponse.failure(
                    GlobalErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    GlobalErrorCode.RESOURCE_NOT_FOUND.getMessage()
            ));
  }

  // 예상치 못한 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Object>>  handleException(Exception ex) {
    log.error("Server 오류 발생: ", ex);

    return ResponseEntity
        .status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(BaseResponse.failure(
            GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        ));
  }
}