package learntime.backend.domain.profile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import lombok.Builder;


@Builder
@Schema(description = "프로필 수정 요청 DTO")
public record ProfileUpdateRequestDTO(
        @Schema(description = "한 줄 소개 (최대 1000자, null 가능)", example = "안녕하세요! 백엔드 개발자입니다.")
        String description,

        @Schema(description = "프로필 공개 여부 (PUBLIC, PRIVATE)", example = "PUBLIC")
        ProfileVisibility profileVisibility,

        @Schema(description = "프로필 이미지를 삭제할지 여부", example = "false")
        Boolean isImageDeleted
) {}

