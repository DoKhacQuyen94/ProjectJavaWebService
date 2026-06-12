package rikkei.management_course.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SubmissionRequest {

    @NotNull(message = "ID bài tập không được để trống")
    private Long assignmentId;

    // Bỏ @NotBlank và @Pattern cũ vì Sinh viên có thể chọn nộp File thay vì link GitHub
    private String githubUrl;

    // THÊM TRƯỜNG FILE ĐỂ THỎA MÃN ĐIỀU KIỆN HỘI ĐỒNG CHẤM
    private MultipartFile file;
}