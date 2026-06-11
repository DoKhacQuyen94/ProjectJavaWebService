package rikkei.management_course.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rikkei.management_course.model.dto.request.CourseRequest;
import rikkei.management_course.model.dto.response.CourseResponse;
import rikkei.management_course.model.entity.Course;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.CourseRepository;
import rikkei.management_course.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    // Lấy danh sách + Tìm kiếm + Phân trang bằng Pageable trực tiếp
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(String keyword, Pageable pageable) {
        Page<Course> coursePage = (keyword != null && !keyword.trim().isEmpty())
                ? courseRepository.findByCourseNameContainingIgnoreCase(keyword, pageable)
                : courseRepository.findAll(pageable);

        // Map trực tiếp từ Page<Course> sang Page<CourseResponse> cực kỳ clean
        return coursePage.map(this::mapToCourseResponse);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        User lecturer = userRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Giảng viên với ID cung cấp"));

        Course course = Course.builder()
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .lecturer(lecturer)
                .build();

        return mapToCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        User lecturer = userRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Giảng viên"));

        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setLecturer(lecturer);

        return mapToCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy khóa học để xóa");
        }
        courseRepository.deleteById(id);
    }

    private CourseResponse mapToCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .lecturerId(course.getLecturer().getId())
                .lecturerName(course.getLecturer().getUsername())
                .build();
    }
}