package learntime.backend.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.exercise.dto.request.MealRequestDTO;
import learntime.backend.domain.exercise.dto.response.MealResponseDTO;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.service.MealService;
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
@RequestMapping("/api/exercise/meal")
@RequiredArgsConstructor
@Tag(name = "운동 식단 API",description = "운동 식단 저장을 담당 (JWT필요)")
public class MealController {

    private final MealService mealService;

    @PostMapping("/save")
    @Operation(summary = "식단 정보 저장", description = "섭취한 식단이 식품영양정보에 존재하는 값일 경우 식품영양정보 값을, 존재하지 않을 경우 Gemini의 추정값을 사용합니다.")
    public ResponseEntity<MealResponseDTO> saveMeal(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody MealRequestDTO request) {

        MealRecord saved = mealService.saveMeal(user.userId(), request);

        MealResponseDTO response = MealResponseDTO.builder()
                .id(saved.getMealRecordId())
                .foodName(saved.getFoodName())
                .calories(saved.getCalories())
                .protein(saved.getProtein())
                .isEstimated(saved.getIsEstimated())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
