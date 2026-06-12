package rikkei.management_course;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.response.LectureMaterialResponse;
import rikkei.management_course.model.entity.Course;
import rikkei.management_course.model.entity.LectureMaterial;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.CourseRepository;
import rikkei.management_course.repository.LectureMaterialRepository;
import rikkei.management_course.repository.UserRepository;
import rikkei.management_course.service.LecturerMaterialService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LecturerMaterialServiceTest {

    @Mock private LectureMaterialRepository materialRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private LecturerMaterialService lecturerMaterialService;

    private User mockLecturer;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        // Giả lập thông tin SecurityContextHolder để lấy Username đăng nhập từ JWT
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("lecturer_quyen");
        SecurityContextHolder.setContext(securityContext);

        // Khởi tạo dữ liệu mẫu để test
        mockLecturer = User.builder().id(1L).username("lecturer_quyen").build();
        mockCourse = Course.builder().id(100L).courseName("Java Web").lecturer(mockLecturer).build();
    }

    // --- TEST CASE 1: Tải học liệu thành công 100% ---
    @Test
    void uploadMaterial_Success() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");

        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.of(mockLecturer));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(mockCourse));

        // Giả lập luồng đẩy file lên Cloudinary trả về map kết quả kèm URL hầm hố
        Map<String, Object> cloudinaryResult = new HashMap<>();
        cloudinaryResult.put("secure_url", "https://res.cloudinary.com/test.pdf");
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(cloudinaryResult);

        LectureMaterial savedEntity = LectureMaterial.builder().id(10L).title("Slide 1").fileUrl("https://res.cloudinary.com/test.pdf").course(mockCourse).build();
        when(materialRepository.save(any(LectureMaterial.class))).thenReturn(savedEntity);

        LectureMaterialResponse response = lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile);

        assertNotNull(response);
        assertEquals("https://res.cloudinary.com/test.pdf", response.getFileUrl());
        verify(materialRepository, times(1)).save(any(LectureMaterial.class));
    }

    // --- TEST CASE 2: File rỗng -> Tung lỗi IllegalArgumentException ---
    @Test
    void uploadMaterial_EmptyFile_ThrowsIllegalArgumentException() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }

    // --- TEST CASE 3: Không tìm thấy Giảng viên đăng nhập trong DB -> Tung lỗi ---
    @Test
    void uploadMaterial_LecturerNotFound_ThrowsResourceNotFoundException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }

    // --- TEST CASE 4: Không tìm thấy Khóa học tương ứng với ID -> Tung lỗi ---
    @Test
    void uploadMaterial_CourseNotFound_ThrowsResourceNotFoundException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.of(mockLecturer));
        when(courseRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }

    // --- TEST CASE 5: Giảng viên chấm lớp/đăng bài lớp của người khác -> Bị chặn 403 ---
    @Test
    void uploadMaterial_AccessDenied_ThrowsAccessDeniedException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.of(mockLecturer));

        // Khóa học này thuộc về một giảng viên khác hoàn toàn (ID = 999)
        User anotherLecturer = User.builder().id(999L).username("lecturer_tam").build();
        Course strangersCourse = Course.builder().id(100L).lecturer(anotherLecturer).build();
        when(courseRepository.findById(100L)).thenReturn(Optional.of(strangersCourse));

        assertThrows(AccessDeniedException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }
}