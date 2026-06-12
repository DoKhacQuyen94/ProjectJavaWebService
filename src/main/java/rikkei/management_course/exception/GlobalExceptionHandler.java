package rikkei.management_course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. NHÓM MÃ LỖI 400 - BAD REQUEST
    // =========================================================================

    // Bắt lỗi Validation kiểm rà đầu vào của các DTO (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    // Bắt lỗi truyền sai định dạng tham số / tệp tin rỗng từ Client
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // =========================================================================
    // 2. NHÓM MÃ LỖI 401 - UNAUTHORIZED
    // =========================================================================

    // Bắt lỗi đăng nhập sai tài khoản hoặc sai mật khẩu hệ thống
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác!");
    }

    // =========================================================================
    // 3. NHÓM MÃ LỖI 403 - FORBIDDEN
    // =========================================================================

    // Bắt lỗi khi người dùng đăng nhập bằng tài khoản đang bị khóa (is_active = 0)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledAccount(DisabledException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa, vui lòng liên hệ Admin!");
    }

    // Bắt lỗi phân quyền: Giảng viên lấn sân lớp khác hoặc Sinh viên truy cập API Admin
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // =========================================================================
    // 4. NHÓM MÃ LỖI 404 - NOT FOUND
    // =========================================================================

    // Bắt lỗi không tìm thấy bất kỳ tài nguyên dữ liệu nào trong DB (Course, User, Submission...)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // =========================================================================
    // 5. NHÓM MÃ LỖI 409 - CONFLICT
    // =========================================================================

    // Bắt lỗi trùng lặp dữ liệu (Username/Email đã có) hoặc vi phạm quy tắc nộp bài/chấm điểm
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ResourceConflictException ex) {
        return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // =========================================================================
    // HÀM TRỢ GIÚP (HELPER): ĐỒNG NHẤT CẤU TRÚC JSON ĐẦU RA CHO TOÀN HỆ THỐNG
    // =========================================================================
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status.value());
        responseBody.put("error", status.getReasonPhrase()); // Tự động bốc chuỗi text (Ví dụ: "Bad Request", "Forbidden")
        responseBody.put("message", message);
        return new ResponseEntity<>(responseBody, status);
    }
}