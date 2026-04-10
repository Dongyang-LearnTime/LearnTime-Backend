package learntime.backend.domain.exercise.controller;

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

import java.security.Principal;

@RestController
@RequestMapping("/api/exercise/meal")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping("/save")
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
                .createAt(saved.getCreateAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
