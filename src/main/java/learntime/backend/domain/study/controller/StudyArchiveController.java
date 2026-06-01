package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.study.dto.response.StudyArchiveResponseDTO;
import learntime.backend.domain.study.service.core.StudyArchiveService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/study/archive")
@RequiredArgsConstructor
@Tag(name = "학습 아카이브 API", description = "탈퇴한 스터디를 포함한 내 학습 이력 조회 API")
public class StudyArchiveController {

    private final StudyArchiveService studyArchiveService;

    @GetMapping
    @Operation(
            summary = "내 스터디 아카이브 목록",
            description = "탈퇴한 스터디를 포함한 전체 참여 이력을 반환합니다."
    )
    public ResponseEntity<List<StudyArchiveResponseDTO>> getMyArchivedStudies(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                studyArchiveService.getMyArchivedStudies(userDetails.userId())
        );
    }

}
