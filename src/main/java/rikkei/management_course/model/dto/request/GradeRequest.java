package rikkei.management_course.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeRequest {

    @NotNull(message = "Điểm số không được để trống")
    @Min(value = 0, message = "Điểm số thấp nhất là 0")
    @Max(value = 100, message = "Điểm số cao nhất là 100")
    private Double score;

    @NotBlank(message = "Lời nhận xét, feedback không được để trống")
    private String feedback;
}