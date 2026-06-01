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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    @Operation(summary = "최근 신체 데이터 목록 조회", description = "사용자의 전체 신체 데이터 목록을 최신순으로 반환합니다.")
    public ResponseEntity<List<WeightResponseDTO>> getRecentWeights(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(weightService.getRecentWeights(userDetails.userId()));
    }

    @DeleteMapping("/{weightRecordId}")
    @Operation(summary = "신체 데이터 삭제", description = "특정 신체 데이터를 삭제합니다.")
    public ResponseEntity<Void> deleteWeight(
            @PathVariable Long weightRecordId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        weightService.deleteWeight(userDetails.userId(), weightRecordId);
        return ResponseEntity.noContent().build();
    }
}
