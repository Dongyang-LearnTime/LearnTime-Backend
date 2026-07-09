package learntime.backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 이메일 전송 요청 DTO")
public record AdminUserEmailRequest(
        @NotBlank(message = "이메일 제목은 필수입니다.")
        @Schema(description = "이메일 제목")
        String subject,
        
        @NotBlank(message = "이메일 내용은 필수입니다.")
        @Schema(description = "이메일 내용")
        String content
) {}
