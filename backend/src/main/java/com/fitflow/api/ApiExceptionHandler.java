package com.fitflow.api;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将表单校验和数据库约束异常转换为可读的客户端错误响应。 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(
    MethodArgumentNotValidException exception
  ) {
    String message = exception
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .findFirst()
      .map(error -> error.getDefaultMessage())
      .orElse("提交内容不符合要求");
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleConflict(
    DataIntegrityViolationException exception
  ) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(Map.of("message", "手机号已存在或数据与现有记录冲突"));
  }
}
