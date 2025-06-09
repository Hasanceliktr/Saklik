package org.hasanceliktr.saklik.exception;

import org.hasanceliktr.saklik.dto.ErrorResponseDto; // ErrorResponseDto'muz
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // AuthenticationException için
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest; // WebRequest yerine HttpServletRequest de kullanılabilir

import java.time.LocalDateTime;
import java.util.HashMap; // Map için
import java.util.Map;   // Map için

@ControllerAdvice // Bu sınıfın global exception handler olduğunu belirtir
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Custom Exception'ların (Eğer varsa, UsernameAlreadyExistsException, EmailAlreadyExistsException gibi)
    // handler'ları burada kalabilir. Onlar doğru görünüyor.
    // Örnek:
    /*
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Kullanıcı adı çakışması: {} - Path: {}", ex.getMessage(), path);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(), // 409 Conflict
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                path
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, WebRequest request) {
        // ... (Benzer şekilde) ...
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponseDto errorResponse = new ErrorResponseDto(LocalDateTime.now(), HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), ex.getMessage(), path);
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    */


    // MethodArgumentNotValidException (DTO validasyon hataları - @Valid) için handler
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        Map<String, String> fieldErrors = new HashMap<>(); // Map<String, String> KULLAN
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                fieldErrors.put(fieldName, errorMessage); // Map'e ekle
            }
        });
        logger.warn("Doğrulama hatası: {} - Path: {} - Detaylar: {}", "Doğrulama hataları bulundu.", path, fieldErrors);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Girilen bilgilerde doğrulama hataları bulundu. Lütfen kontrol edin.", // Genel mesaj
                path
        );
        if (!fieldErrors.isEmpty()) {
            errorResponse.setValidationErrors(fieldErrors); // Map'i set et
        }
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Bizim AuthService'ten fırlattığımız genel RuntimeException'lar için
    // ÖNEMLİ: Eğer UsernameAlreadyExistsException gibi daha spesifik exception'ların varsa,
    // bu genel RuntimeException handler'ı onlardan SONRA gelmeli veya
    // bu handler daha spesifik olmayan RuntimeException'ları yakalamalı.
    // Veya RuntimeException yerine custom bir BusinessLogicException gibi bir base class kullanıp onu yakalayabilirsin.
    // Şimdilik, "Kullanıcı adı zaten kullanımda" gibi hatalar buraya düşebilir.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleGenericRuntimeException(RuntimeException ex, WebRequest request) {
        // UsernameAlreadyExistsException veya EmailAlreadyExistsException gibi spesifik olanlar
        // zaten kendi handler'ları tarafından yakalanacak, bu yüzden bu metod onları tekrar yakalamaz.
        // Ancak, eğer o spesifik handler'lar yoksa veya farklı bir RuntimeException fırlatılırsa bu çalışır.
        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("İş mantığı hatası (RuntimeException): {} - Path: {}", ex.getMessage(), path, ex);

        // Bu tür hatalar için 400 Bad Request veya 409 Conflict daha uygun olabilir.
        // Duruma göre karar verilmeli. Bizim "Kullanıcı adı zaten kullanımda" hatamız aslında bir CONFLICT.
        // Eğer spesifik handler'ların yoksa, burada mesaj içeriğine göre status kodu değiştirebilirsin.
        HttpStatus status = HttpStatus.BAD_REQUEST; // Varsayılan
        if (ex.getMessage() != null && (ex.getMessage().contains("zaten kullanımda") || ex.getMessage().contains("already exists"))) {
            status = HttpStatus.CONFLICT; // 409
        }

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }


    // Spring Security AuthenticationException'ları için (örn: BadCredentialsException)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Kimlik doğrulama hatası: {} - Path: {}", ex.getMessage(), path);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Kimlik doğrulama başarısız oldu. Lütfen kullanıcı adı ve şifrenizi kontrol edin.", // Daha genel mesaj
                path
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }


    // Diğer tüm yakalanmayan Exception'lar için genel bir fallback handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllUncaughtException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        logger.error("Beklenmeyen genel bir hata oluştu - Path: {} - Hata: {}", path, ex.getMessage(), ex);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Sunucuda beklenmeyen bir teknik hata oluştu. Lütfen daha sonra tekrar deneyin.",
                path
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}