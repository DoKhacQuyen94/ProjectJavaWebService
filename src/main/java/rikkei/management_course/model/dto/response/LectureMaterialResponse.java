package rikkei.management_course.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LectureMaterialResponse {
    private Long id;
    private String title;
    private String fileUrl;
    private Long courseId;
    private String courseName;
}