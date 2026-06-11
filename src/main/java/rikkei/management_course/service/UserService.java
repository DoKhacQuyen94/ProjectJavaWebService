package rikkei.management_course.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rikkei.management_course.exception.ResourceConflictException;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.request.*;
import rikkei.management_course.model.dto.response.JwtResponse;
import rikkei.management_course.model.dto.response.UserResponse;
import rikkei.management_course.model.entity.RoleEnum;
import rikkei.management_course.model.entity.TokenBlacklist;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.TokenBlacklistRepository;
import rikkei.management_course.repository.UserRepository;
import rikkei.management_course.security.JwtTokenProvider;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistRepository blacklistRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Transactional
    public UserResponse registerStudent(RegisterRequest request) {
        // 1. Kiểm tra trùng username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceConflictException("Username này đã tồn tại trên hệ thống!");
        }

        // 2. Kiểm tra trùng email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email này đã tồn tại trên hệ thống!");
        }

        // 3. Khởi tạo thực thể User & mã hóa mật khẩu qua BCrypt
        User student = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(RoleEnum.Student) // Gán cứng Role STUDENT cho luồng đăng ký này
                .isActive(true)
                .build();

        User savedUser = userRepository.save(student);

        // 4. Mapping dữ liệu sang DTO trả về để bảo mật cấu trúc Entity
        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .active(savedUser.isActive())
                .build();
    }
    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest loginRequest) {
        // 1. Xác thực tài khoản & mật khẩu qua Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. Lưu thông tin vào Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Khởi tạo cặp bài trùng AccessToken và RefreshToken
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshString(authentication);

        // 4. Thao tác với Repository đúng layer để lấy data mở rộng
        User userEntity = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng sau khi xác thực"));

        // 5. Trả về DTO đóng gói sạch đẹp
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(userEntity.getUsername())
                .email(userEntity.getEmail())
                .role(userEntity.getRole().name())
                .build();
    }
    @Transactional
    public JwtResponse refreshToken(TokenRefreshRequest request) {
        String currentRefreshToken = request.getRefreshToken();

        // 1. Kiểm tra cấu trúc/hạn dùng của chuỗi JWT Refresh Token
        if (!tokenProvider.validateToken(currentRefreshToken)) {
            throw new BadCredentialsException("Refresh Token không hợp lệ hoặc đã hết hạn!");
        }

        // 2. Trích xuất Username từ Refresh Token
        String username = tokenProvider.getUsernameFromJWT(currentRefreshToken);

        // 3. Kiểm tra xem Token này đã nằm trong Blacklist chưa (Chống trộm token - UC-03)
        if (blacklistRepository.existsByToken(currentRefreshToken)) {
            throw new BadCredentialsException("Refresh Token này đã bị thu hồi hoặc đã được sử dụng trước đó!");
        }

        // 4. Nạp lại thông tin UserDetails từ Service để kiểm tra trạng thái tài khoản
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!userDetails.isEnabled()) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa, không thể refresh token!");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // 5. Tiến hành xoay vòng: Đưa Refresh Token vừa dùng vào danh sách đen (UC-03)
        TokenBlacklist badToken = TokenBlacklist.builder()
                .token(currentRefreshToken)
                .expiryDate(LocalDateTime.now().plusDays(7)) // Khớp thời hạn sống của Refresh Token
                .build();
        blacklistRepository.save(badToken);

        // 6. Tạo mới hoàn toàn cặp AccessToken và RefreshToken thế hệ tiếp theo (Xoay vòng thành công)
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshString(authentication);

        // 7. Lấy thông tin user entity để đóng gói dữ liệu trả về
        User userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng hợp lệ"));

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(userEntity.getUsername())
                .email(userEntity.getEmail())
                .role(userEntity.getRole().name())
                .build();
    }
    @Transactional
    public void logout(String token) {
        // 1. Kiểm tra nếu token trống hoặc không hợp lệ cấu trúc/hạn dùng
        if (token == null || !tokenProvider.validateToken(token)) {
            throw new BadCredentialsException("Token không hợp lệ hoặc đã hết hạn!");
        }

        // 2. Kiểm tra xem token này đã từng gọi logout trước đây chưa
        if (blacklistRepository.existsByToken(token)) {
            throw new BadCredentialsException("Token này đã bị thu hồi trước đó!");
        }

        // 3. Tiến hành đưa token vào danh sách đen
        // Thiết lập thời gian hết hạn tạm thời là +1 ngày (hoặc tối ưu bằng cách trích xuất chuẩn thời gian exp của JWT)
        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();

        blacklistRepository.save(blacklistedToken);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResourceConflictException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nào liên kết với Email này!"));

        // Tạo một SecretKey động: Kết hợp Secret mặc định + Mật khẩu băm hiện tại của User
        // Việc này đảm bảo nếu mật khẩu thay đổi, token sẽ hết hiệu lực ngay lập tức!
        String dynamicSecret = jwtSecret + user.getPassword();
        Key key = Keys.hmacShaKeyFor(dynamicSecret.getBytes(StandardCharsets.UTF_8));

        // Tạo chuỗi mã hóa resetToken có tuổi thọ ngắn (15 phút) bằng cú pháp JJWT 0.11.5
        String resetToken = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 phút
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        System.out.println("============== EMAIL RESET PASSWORD STATELESS =============");
        System.out.println("Gửi tới: " + user.getEmail());
        System.out.println("Token Reset (Stateless JWT): " + resetToken);
        System.out.println("===========================================================");

        return "Hệ thống đã gửi liên kết đặt lại mật khẩu tới Email của bạn!";
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        String username;

        try {
            // Vì không dùng DB, trước tiên ta bóc tách phần thân JWT "không cần verify" để lấy username ra trước
            int i = token.lastIndexOf('.');
            String withoutSignature = token.substring(0, i + 1);
            Claims claims = Jwts.parserBuilder().build()
                    .parseClaimsJwt(withoutSignature).getBody();
            username = claims.getSubject();
        } catch (Exception e) {
            throw new BadCredentialsException("Mã Token xác nhận không đúng định dạng!");
        }

        // Tìm user từ username vừa bóc tách được
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Mã xác nhận không hợp lệ cho người dùng này!"));

        try {
            // Tái cấu trúc lại SecretKey động bằng mật khẩu hiện tại của user để tiến hành verify chính thức
            String dynamicSecret = jwtSecret + user.getPassword();
            Key key = Keys.hmacShaKeyFor(dynamicSecret.getBytes(StandardCharsets.UTF_8));

            // Kiểm tra xem token có bị sửa đổi hoặc hết hạn 15 phút chưa
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

        } catch (ExpiredJwtException e) {
            throw new BadCredentialsException("Mã xác nhận này đã hết hạn sử dụng (quá 15 phút)!");
        } catch (Exception e) {
            throw new BadCredentialsException("Mã xác nhận không hợp lệ hoặc đã từng được sử dụng trước đó!");
        }

        // Nếu verify thành công -> Cập nhật mật khẩu mới xịn (Mật khẩu hash thay đổi -> Token cũ tự phế võ công)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}