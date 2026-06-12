package rikkei.management_course.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Role không được để trống")
    private String role;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;
}