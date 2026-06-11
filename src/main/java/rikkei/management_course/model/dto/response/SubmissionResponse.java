package rikkei.management_course.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionResponse {
    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private String studentUsername;
    private String reportUrl; // Đây sẽ lưu link GitHub gửi lên
    private String status;    // SUBMITTED hoặc LATE
    private Double score;
    private String feedback;
}