package rikkei.management_course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi trùng lặp dữ liệu -> Trả về 409 Conflict chuẩn SRS
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ResourceConflictException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // Bắt lỗi Validation đầu vào (DTO) -> Trả về 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");

        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        body.put("message", errorMessage);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    // Bắt lỗi Không tìm thấy tài nguyên liệu -> Trả về 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
    // Bắt lỗi sai tài khoản hoặc mật khẩu -> Trả về 401 Unauthorized
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException ex) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("status", org.springframework.http.HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Tài khoản hoặc mật khẩu không chính xác!");
        return new ResponseEntity<>(body, org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    // Bắt lỗi tài khoản bị khóa (is_active = 0) -> Trả về 403 Forbidden
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledAccount(org.springframework.security.authentication.DisabledException ex) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("status", org.springframework.http.HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Tài khoản của bạn đã bị khóa, vui lòng liên hệ Admin!");
        return new ResponseEntity<>(body, org.springframework.http.HttpStatus.FORBIDDEN);
    }
}