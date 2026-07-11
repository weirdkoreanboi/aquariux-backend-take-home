package com.aquariux.technical.assessment.trade.exception;

import com.aquariux.technical.assessment.trade.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TradeException.class)
    public ResponseEntity<ApiErrorResponse> handleTradeException(TradeException exception,
                                                                  HttpServletRequest request) {
        return error(exception.getStatus(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                              HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception,
                                                                     HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request or unsupported tradeType", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message,
                                                    HttpServletRequest request,
                                                    Map<String, String> validationErrors) {
        ApiErrorResponse body = new ApiErrorResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), validationErrors);
        return ResponseEntity.status(status).body(body);
    }
}
