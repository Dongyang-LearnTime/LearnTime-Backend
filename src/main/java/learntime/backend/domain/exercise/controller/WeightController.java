package learntime.backend.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.dto.response.WeightResponseDTO;
import learntime.backend.domain.exercise.service.WeightService;
import learntime.backend.global.security.CustomUserDetails;
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
@Tag(name = "신체 데이터 API", description = "신체데이터에 해당하는 체지방량과 몸무게 저장을 담당 (JWT 필요)")
public class WeightController {
    private final WeightService weightService;

    @PostMapping("/save")
    @Operation(summary = "신체 데이터 저장", description = "체중과 체지방량을 입력하면, 해당 내용이 암호화되어 DB에 저장됩니다.")
    public ResponseEntity<WeightResponseDTO> saveWeight(@RequestBody WeightRequestDTO request,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        WeightResponseDTO result = weightService.saveWeight(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
