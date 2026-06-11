package rikkei.management_course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rikkei.management_course.exception.ResourceConflictException;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.response.CourseResponse;
import rikkei.management_course.model.entity.Course;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.CourseRepository;
import rikkei.management_course.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class StudentCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseResponse enrollInCourse(Long courseId) {
        // 1. Lấy username của Sinh viên đang đăng nhập từ JWT Token bảo mật
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản sinh viên"));

        // 2. Kiểm tra khóa học có tồn tại hay không
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học không tồn tại hoặc đã bị xóa"));

        // 3. Kiểm tra xem sinh viên đã đăng ký khóa học này chưa
        // Vì Course là bên sở hữu @ManyToMany (đặt @JoinTable), ta thao tác trên collection của Course
        if (course.getStudents().contains(student)) {
            throw new ResourceConflictException("Bạn đã đăng ký tham gia khóa học này rồi!");
        }

        // 4. Thêm sinh viên vào khóa học và lưu xuống CSDL
        course.getStudents().add(student);
        courseRepository.save(course);

        // 5. Trả về thông tin khóa học vừa đăng ký thành công dưới dạng DTO sạch
        return CourseResponse.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .lecturerId(course.getLecturer().getId())
                .lecturerName(course.getLecturer().getUsername())
                .build();
    }
}