package learntime.backend.domain.exercise.controller;

import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.dto.response.WeightResponseDTO;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.exercise.service.WeightService;
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
@RequestMapping("/api/exercise/weight")
@RequiredArgsConstructor
public class WeightController {
    private final WeightService weightService;

    @PostMapping("/save")
    public ResponseEntity<WeightResponseDTO> saveWeight(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody WeightRequestDTO request) {

        WeightRecord saved = weightService.saveWeight(user.userId(), request);

        WeightResponseDTO response = WeightResponseDTO.builder()
                .id(saved.getWeightRecordId())
                .weight(saved.getWeight())
                .bodyFat(saved.getBodyFat())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
