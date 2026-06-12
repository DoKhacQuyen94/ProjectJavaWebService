package rikkei.management_course;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
        // Cấu hình SecurityContext giả lập
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        // Sử dụng lenient() để tránh lỗi UnnecessaryStubbingException khi chạy test case file trống
        Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.lenient().when(authentication.getName()).thenReturn("lecturer_quyen");
        SecurityContextHolder.setContext(securityContext);

        mockLecturer = User.builder().id(1L).username("lecturer_quyen").build();
        mockCourse = Course.builder().id(100L).courseName("Java Web").lecturer(mockLecturer).build();
    }

    // T1: Upload thành công
    @Test
    void uploadMaterial_Success() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");

        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.of(mockLecturer));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(mockCourse));

        Map<String, Object> cloudinaryResult = new HashMap<>();
        cloudinaryResult.put("secure_url", "https://res.cloudinary.com/test.pdf");
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(cloudinaryResult);

        LectureMaterial savedEntity = LectureMaterial.builder()
                .id(10L).title("Slide 1").fileUrl("https://res.cloudinary.com/test.pdf").course(mockCourse).build();
        when(materialRepository.save(any(LectureMaterial.class))).thenReturn(savedEntity);

        LectureMaterialResponse response = lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile);

        assertNotNull(response);
        assertEquals("https://res.cloudinary.com/test.pdf", response.getFileUrl());
        verify(materialRepository, times(1)).save(any(LectureMaterial.class));
    }

    // T2: File rỗng ném lỗi 400
    @Test
    void uploadMaterial_EmptyFile_ThrowsIllegalArgumentException() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }

    // T3: Không tìm thấy giảng viên
    @Test
    void uploadMaterial_LecturerNotFound_ThrowsResourceNotFoundException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }

    // T4: Không tìm thấy khóa học
    @Test
    void uploadMaterial_CourseNotFound_ThrowsResourceNotFoundException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.of(mockLecturer));
        when(courseRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }

    // T5: Giảng viên lấn sân lớp khác bị chặn 403
    @Test
    void uploadMaterial_AccessDenied_ThrowsAccessDeniedException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(userRepository.findByUsername("lecturer_quyen")).thenReturn(Optional.of(mockLecturer));

        User anotherLecturer = User.builder().id(999L).username("lecturer_tam").build();
        Course strangersCourse = Course.builder().id(100L).lecturer(anotherLecturer).build();
        when(courseRepository.findById(100L)).thenReturn(Optional.of(strangersCourse));

        assertThrows(AccessDeniedException.class, () ->
                lecturerMaterialService.uploadMaterial("Slide 1", 100L, multipartFile)
        );
    }
}