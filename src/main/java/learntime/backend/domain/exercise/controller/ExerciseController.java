package learntime.backend.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseResponseDTO;
import learntime.backend.domain.exercise.dto.response.WeeklyWeightStatsResponseDTO;
import learntime.backend.domain.exercise.service.ExerciseService;
import learntime.backend.global.security.CustomUserDetails;
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
    @Operation(summary = "추천 운동 영상 조회", description = "사용자가 선택한 신체부위에 해당하는 Youtube id를 조회한다.")
    public ResponseEntity<List<YoutubeVideoResponseDTO>> getVideos(@RequestParam List<String> bodyParts) {
        List<YoutubeVideoResponseDTO> videos = exerciseService.getRecommendedVideos(bodyParts);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/recent-week")
    @Operation(
            summary = "최근 일주일 동안 든 무게 수",
            description = "사용자의 최근 일주일 간 무게 수를 조회하여, 일자별로 총 무게수를 조회한다.")
    public ResponseEntity<List<WeeklyWeightStatsResponseDTO>> getWeeklyWeightMetrics (@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<WeeklyWeightStatsResponseDTO> result = exerciseService.getRecentWeeklyWeightStats(userDetails.userId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    @Operation(summary = "운동 기록 저장", description = "운동 부위, 시간(분), 내용, 중량을 저장한다.")
    public ResponseEntity<ExerciseResponseDTO> saveExercise(@Valid @RequestBody ExerciseRequestDTO request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ExerciseResponseDTO result = exerciseService.saveExercise(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Operation(summary = "전체 운동 기록 조회", description = "사용자의 전체 운동 기록을 최신순으로 조회한다.")
    public ResponseEntity<List<ExerciseResponseDTO>> getExercises(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(exerciseService.getExercises(userDetails.userId()));
    }

    @GetMapping("/{exerciseRecordId}")
    @Operation(summary = "단일 운동 기록 상세 조회", description = "특정 운동 기록의 상세 내용을 조회한다.")
    public ResponseEntity<ExerciseResponseDTO> getExercise(
            @PathVariable Long exerciseRecordId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(exerciseService.getExercise(userDetails.userId(), exerciseRecordId));
    }

    @PutMapping("/{exerciseRecordId}")
    @Operation(summary = "운동 기록 수정", description = "운동 기록을 수정한다. 칼로리 관련 항목이 변경되면 소모 칼로리를 재계산한다.")
    public ResponseEntity<ExerciseResponseDTO> updateExercise(
            @PathVariable Long exerciseRecordId,
            @Valid @RequestBody ExerciseRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ExerciseResponseDTO result = exerciseService.updateExercise(userDetails.userId(), exerciseRecordId, request);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{exerciseRecordId}")
    @Operation(summary = "운동 기록 삭제", description = "특정 운동 기록을 삭제한다.")
    public ResponseEntity<Void> deleteExercise(
            @PathVariable Long exerciseRecordId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        exerciseService.deleteExercise(userDetails.userId(), exerciseRecordId);
        return ResponseEntity.noContent().build();
    }
}
