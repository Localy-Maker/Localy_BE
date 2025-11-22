package org.example.localy.common.exception.model;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

  String getCode();

  String getMessage();

  HttpStatus getStatus();
}