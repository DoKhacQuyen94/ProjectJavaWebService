package rikkei.management_course;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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

@WebMvcTest(LecturerMaterialController.class)
@AutoConfigureMockMvc(addFilters = false) // Vô hiệu hóa Filter Spring Security để tập trung test logic Controller
class LecturerMaterialControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean
    private LecturerMaterialService materialService;

    // --- TEST CASE 6: Gọi API upload thành công -> Trả về 201 Created ---
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

    // --- TEST CASE 7: Khóa học không tồn tại -> Service ném lỗi 404 -> Trả về 404 Not Found ---
    @Test
    void uploadMaterial_Controller_CourseNotFound_Returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

        when(materialService.uploadMaterial(any(), any(), any())).thenThrow(new ResourceNotFoundException("Khóa học không tồn tại"));

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "99")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound()); // Đảm bảo GlobalExceptionHandler map về đúng 404
    }

    // --- TEST CASE 8: Giảng viên lấn quyền sang lớp khác -> Service ném AccessDenied -> Trả về 403 ---
    @Test
    void uploadMaterial_Controller_AccessDenied_Returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

        when(materialService.uploadMaterial(any(), any(), any())).thenThrow(new AccessDeniedException("Không có quyền"));

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden()); // Kiểm tra trả về mã 403 Forbidden
    }

    // --- TEST CASE 9: File rỗng -> Service ném lỗi IllegalArgumentException -> Trả về 400 ---
    @Test
    void uploadMaterial_Controller_EmptyFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        when(materialService.uploadMaterial(any(), any(), any())).thenThrow(new IllegalArgumentException("File trống"));

        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .param("courseId", "2")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Kiểm tra trả về mã 400 Bad Request
    }

    // --- TEST CASE 10: Request thiếu tham số đầu vào bắt buộc (Thiếu courseId) -> Spring chặn trả về 400 ---
    @Test
    void uploadMaterial_Controller_MissingParam_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

        // Thực hiện gửi request có file và title nhưng cố tình bỏ quên trường param "courseId"
        mockMvc.perform(multipart("/api/v1/lecturer/materials")
                        .file(file)
                        .param("title", "Slide OOP")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Thiếu param hệ thống tự trả về 400 Bad Request mà không cần vào Service
    }
}