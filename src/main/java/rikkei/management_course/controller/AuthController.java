package rikkei.management_course.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rikkei.management_course.model.dto.request.*;
import rikkei.management_course.model.dto.response.JwtResponse;
import rikkei.management_course.model.dto.response.UserResponse;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.security.JwtTokenProvider;
import rikkei.management_course.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerStudent(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.registerStudent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = userService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshTheToken(@Valid @RequestBody TokenRefreshRequest request) {
        // Gọi xuống Service xử lý cấp bù Token
        JwtResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
    // Post /api/v1/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        // 1. Lấy chuỗi Authorization từ Header
        String bearerToken = request.getHeader("Authorization");
        String jwt = null;

        // 2. Bóc tách tiền tố "Bearer " để lấy chuỗi token nguyên bản
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            jwt = bearerToken.substring(7);
        }

        // 3. Gọi xuống Service để tước quyền truy cập của Token
        userService.logout(jwt);

        // 4. Xóa sạch Security Context hiện tại của Thread để đảm bảo an toàn Stateless
        SecurityContextHolder.clearContext();

        // 5. Phản hồi về mã HTTP 200 OK kèm thông báo sạch theo chuẩn RESTful
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đăng xuất thành công, token đã được thu hồi hoàn toàn!");

        return ResponseEntity.ok(response);
    }


    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Thay đổi mật khẩu thành công!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = userService.forgotPassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đặt lại mật khẩu mới thành công! Bạn có thể đăng nhập ngay bây giờ.");
        return ResponseEntity.ok(response);
    }
}