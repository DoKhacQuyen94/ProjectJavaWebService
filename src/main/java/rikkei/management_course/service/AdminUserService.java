package rikkei.management_course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rikkei.management_course.exception.ResourceConflictException;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.request.UserCreateRequest;
import rikkei.management_course.model.dto.request.UserUpdateRequest;
import rikkei.management_course.model.dto.response.UserResponse;
import rikkei.management_course.model.entity.RoleEnum;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.SubmissionRepository;
import rikkei.management_course.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String keyword, Pageable pageable) {
        Page<User> userPage = (keyword != null && !keyword.trim().isEmpty())
                ? userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable)
                : userRepository.findAll(pageable);

        return userPage.map(user -> UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .build());
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setActive(false); // Khóa tài khoản
        userRepository.save(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceConflictException("Username này đã tồn tại trên hệ thống!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email này đã được đăng ký tài khoản khác!");
        }

        // Khởi tạo thực thể và băm mật khẩu chuẩn BCrypt xịn
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(RoleEnum.valueOf(String.valueOf(request.getRole())))
                .isActive(true) // Mặc định tạo xong là active luôn
                .build();

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng cần cập nhật"));

        // Kiểm tra xem email mới có bị trùng với người khác không
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email mới này đã trùng với một tài khoản khác!");
        }

        user.setEmail(request.getEmail());
        user.setRole(RoleEnum.valueOf(request.getRole().toUpperCase()));
        user.setActive(request.getIsActive());

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng trên hệ thống"));

        // 1. Rào chắn bảo vệ nếu xóa tài khoản LECTURER
        if (user.getRole() == RoleEnum.Lecturer && userRepository.countCoursesByLecturerId(id) > 0) {
            throw new ResourceConflictException("Không thể xóa Giảng viên này vì họ đang có lớp học phụ trách!");
        }

        // 2. FIX LỖI 1451: Rào chắn bảo vệ nếu xóa tài khoản STUDENT
        if (user.getRole() == RoleEnum.Student && submissionRepository.countByStudentId(id) > 0) {
            throw new ResourceConflictException("Không thể xóa Sinh viên này vì họ đã có lịch sử nộp bài tập / đồ án!");
        }

        // Nếu vượt qua hết rào chắn an toàn, tiến hành xóa cứng
        userRepository.delete(user);
    }

    // Hàm helper map nhanh sang DTO phản hồi gọn sạch
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .build();
    }
}