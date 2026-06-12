package rikkei.management_course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rikkei.management_course.controller.LecturerMaterialController;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.response.LectureMaterialResponse;
import rikkei.management_course.service.LecturerMaterialService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LecturerMaterialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LecturerMaterialService materialService;

    @InjectMocks
    private LecturerMaterialController lecturerMaterialController;

    @BeforeEach
    void setUp() {
        // Khởi tạo các đối tượng Mock độc lập
        MockitoAnnotations.openMocks(this);

        // Build MockMvc thủ công bọc quanh Controller, tự động bỏ qua toàn bộ Filter Security gây nhiễu
        this.mockMvc = MockMvcBuilders.standaloneSetup(lecturerMaterialController).build();
    }

    // T6: Gọi API thành công -> Trả về 201 Created
    @Test
    void uploadMaterial_Controller_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());
        LectureMaterialResponse mockResponse = LectureMaterialResponse.builder()
                .id(1L).title("Slide OOP").fileUrl("https://cloudinary/slide.pdf").courseId(2L).courseName("Java Core").build();

        when(materialService.uploadMaterial(eq("Slide OOP"), eq(2L), any())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileUrl").value("https://cloudinary/slide.pdf"));
    }

    // T7: Khóa học không tồn tại -> Trả về 404
    @Test
    void uploadMaterial_Controller_CourseNotFound_Returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());
        when(materialService.uploadMaterial(any(), any(), any())).thenThrow(new ResourceNotFoundException("Khóa học không tồn tại"));

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "99")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    // T8: Sai quyền sở hữu khóa học -> Trả về 403
    @Test
    void uploadMaterial_Controller_AccessDenied_Returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());
        when(materialService.uploadMaterial(any(), any(), any())).thenThrow(new AccessDeniedException("Không có quyền"));

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }

    // T9: File tải lên trống -> Trả về 400
    @Test
    void uploadMaterial_Controller_EmptyFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", MediaType.APPLICATION_PDF_VALUE, new byte[0]);
        when(materialService.uploadMaterial(any(), any(), any())).thenThrow(new IllegalArgumentException("File trống"));

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    // T10: Thiếu tham số truyền lên -> Trả về 400
    @Test
    void uploadMaterial_Controller_MissingParam_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }
}