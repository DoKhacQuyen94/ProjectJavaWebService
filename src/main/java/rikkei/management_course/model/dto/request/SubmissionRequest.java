package rikkei.management_course.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SubmissionRequest {

    @NotNull(message = "ID bài tập không được để trống")
    private Long assignmentId;

    @NotBlank(message = "Link GitHub không được để trống")
    @Pattern(
            regexp = "^(https:\\/\\/)?(www\\.)?github\\.com\\/[a-zA-Z0-9_-]+\\/[a-zA-Z0-9_-]+.*$",
            message = "Đường dẫn phải là một link GitHub hợp lệ (Ví dụ: https://github.com/username/repository)"
    )
    private String githubUrl;
}