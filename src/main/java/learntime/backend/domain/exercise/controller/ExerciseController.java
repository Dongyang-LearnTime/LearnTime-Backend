package learntime.backend.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseResponseDTO;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.service.ExerciseService;
import learntime.backend.global.dto.CustomUserDetails;
import learntime.backend.global.dto.YoutubeVideoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercise")
@RequiredArgsConstructor
@Tag(name = "운동 기능 API", description = "추천 유튜브 영상을 가져오고, 수행한 운동 저장을 담당 (JWT 필요)")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/videos")
    @Operation(summary = "추천 운동 영상 조회", description = "사용자가 선택한 신체부위에 해당하는 Youtube id를 조회합니다.")
    public ResponseEntity<List<YoutubeVideoResponseDTO>> getVideos(@RequestParam List<String> bodyParts) {
        List<YoutubeVideoResponseDTO> videos = exerciseService.getRecommendedVideos(bodyParts);
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/save")
    @Operation(summary = "운동 기록 저장", description = "운동 부위, 시간(분), 내용을 저장합니다.")
    public ResponseEntity<ExerciseResponseDTO> saveExercise(@Valid @RequestBody ExerciseRequestDTO request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ExerciseResponseDTO result = exerciseService.saveExercise(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
