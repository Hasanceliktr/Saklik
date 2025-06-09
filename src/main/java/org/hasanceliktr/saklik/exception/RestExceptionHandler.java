package org.hasanceliktr.saklik.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.hasanceliktr.saklik.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        logger.warn("Validation error for request {}: {}", request.getRequestURI(), errors);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Doğrulama hataları oluştu.",
                request.getRequestURI()
        );
        errorResponse.setValidationErrors(errors);
        return errorResponse;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleGenericRuntimeException(RuntimeException ex, HttpServletRequest request) {
        logger.warn("Runtime exception for request {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponseDto handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        logger.warn("Authentication failure for request {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Kimlik doğrulama başarısız: " + ex.getMessage(),
                request.getRequestURI()
        );
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDto handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception for request {}: {}", request.getRequestURI(), ex.getMessage(), ex); // Tam stack trace'i logla
        return new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Sunucuda beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.", // Kullanıcıya gösterilecek genel mesaj
                request.getRequestURI()
        );
    }
}