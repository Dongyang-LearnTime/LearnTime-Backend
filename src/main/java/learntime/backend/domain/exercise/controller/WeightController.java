package learntime.backend.domain.exercise.controller;

import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.dto.response.WeightResponseDTO;
import learntime.backend.domain.exercise.entity.WeightRecord;
import learntime.backend.domain.exercise.service.WeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/user/weight")
@RequiredArgsConstructor
public class WeightController {
    private final WeightService weightService;

    @PostMapping("/save")
    public ResponseEntity<WeightResponseDTO> saveWeight(
            Principal principal,
            @RequestBody WeightRequestDTO request) {

        WeightRecord saved = weightService.saveWeight(principal.getName(), request);

        WeightResponseDTO response = WeightResponseDTO.builder()
                .id(saved.getId())
                .weight(saved.getWeight())
                .bodyFat(saved.getBodyFat())
                .createAt(saved.getCreateAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
