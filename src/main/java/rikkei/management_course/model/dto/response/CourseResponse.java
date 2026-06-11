package rikkei.management_course.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private String courseName;
    private String description;
    private Long lecturerId;
    private String lecturerName;
}