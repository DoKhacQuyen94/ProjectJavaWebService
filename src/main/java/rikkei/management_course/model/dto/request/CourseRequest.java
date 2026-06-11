package rikkei.management_course.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseRequest {
    @NotBlank(message = "Tên khóa học không được để trống")
    private String courseName;

    private String description;

    @NotNull(message = "ID Giảng viên không được để trống")
    private Long lecturerId;
}