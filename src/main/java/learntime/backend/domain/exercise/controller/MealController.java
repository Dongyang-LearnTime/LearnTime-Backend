package learntime.backend.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.exercise.dto.request.MealRequestDTO;
import learntime.backend.domain.exercise.dto.response.MealResponseDTO;
import learntime.backend.domain.exercise.service.MealService;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.global.annotation.RequireTerms;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercise/meal")
@RequiredArgsConstructor
@RequireTerms(Terms.BODY_DATA_COLLECT)
@Tag(name = "운동 식단 API",description = "운동 식단 저장을 담당 (JWT필요)")
public class MealController {

    private final MealService mealService;

    @PostMapping("/save")
    @Operation(
            summary = "식단 정보 저장",
            description = "섭취한 식단이 식품영양정보에 존재하는 값일 경우 식품영양정보 값을, 존재하지 않을 경우 Gemini의 추정값을 사용합니다."
    )
    public ResponseEntity<MealResponseDTO> saveMeal(@Valid @RequestBody MealRequestDTO request,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        MealResponseDTO result = mealService.saveMeal(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/today")
    @Operation(summary = "오늘의 식단 조회", description = "오늘 하루 동안 저장된 식단 목록을 조회합니다.")
    public ResponseEntity<List<MealResponseDTO>> getTodayMeals(@AuthenticationPrincipal CustomUserDetails userDetails) {

        List<MealResponseDTO> meals = mealService.getTodayMeals(userDetails.userId());
        return ResponseEntity.ok(meals);
    }

    @DeleteMapping("/{mealRecordId}")
    @Operation(summary = "식단 단일 기록 삭제", description = "식단 정보 1개를 삭제합니다.")
    public ResponseEntity<Void> deleteMealRecord(@PathVariable Long mealRecordId,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {

        mealService.deleteMealRecord(mealRecordId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }


}
