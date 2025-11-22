package org.example.localy.common.exception;

import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;

@Getter
public class CustomException extends RuntimeException {

  private final BaseErrorCode errorCode;

  public CustomException(BaseErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}