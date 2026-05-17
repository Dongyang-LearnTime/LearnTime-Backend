package learntime.backend.domain.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.study.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study.service.core.StudyUserContentService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/daily/content")
@RequiredArgsConstructor
@Tag(name = "일일 진도 내용 API", description = "사용자가 직접 입력하는 일일 진도 내용(필기 등) 관련 API")
public class StudyDailyController {

    private final StudyUserContentService studyUserContentService;

    @PostMapping
    @Operation(summary = "일일 진도 내용 추가/수정", description = "특정 일차의 공부 내용을 추가하거나 수정합니다. 저장 시 해당 계획의 상태가 '진행 중'으로 변경됩니다.")
    public ResponseEntity<Long> upsertUserContent(@Valid @RequestBody StudyUserContentRequestDTO request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studyUserContentId = studyUserContentService.upsertUserContent(request, userDetails.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(studyUserContentId);
    }
}
