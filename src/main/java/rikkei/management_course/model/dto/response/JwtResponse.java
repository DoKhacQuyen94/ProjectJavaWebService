package rikkei.management_course.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private final String tokenType = "Bearer";
    private String username;
    private String email;
    private String role;
}