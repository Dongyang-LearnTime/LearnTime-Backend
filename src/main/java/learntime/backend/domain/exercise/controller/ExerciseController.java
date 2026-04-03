package learntime.backend.domain.exercise.controller;

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
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/videos")
    public ResponseEntity<List<YoutubeVideoResponseDTO>> getVideos(@RequestParam List<String> bodyParts) {
        List<YoutubeVideoResponseDTO> videos = exerciseService.getRecommendedVideos(bodyParts);
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/save")
    public ResponseEntity<ExerciseResponseDTO> saveExercise(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ExerciseRequestDTO request
    ) {

        ExerciseRecord saved = exerciseService.saveExercise(user.userId(), request);

        // 엔티티를 DTO로 변환
        ExerciseResponseDTO response = ExerciseResponseDTO.builder()
                .id(saved.getExerciseRecordId())
                .bodyParts(saved.getBodyParts())
                .duration(saved.getDuration())
                .content(saved.getContent())
                .calories(saved.getCalories())
                .createAt(saved.getCreateAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
