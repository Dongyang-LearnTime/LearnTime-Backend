package learntime.backend.domain.calendar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.calendar.dto.request.RoutineRequestDTO;
import learntime.backend.domain.calendar.dto.response.RoutineResponseDTO;
import learntime.backend.domain.calendar.service.RoutineService;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/routines")
@RequiredArgsConstructor
@Tag(name = "루틴 API", description = "반복 일정(루틴) 관련 CRUD API (JWT 필요)")
public class RoutineController {

    private final RoutineService routineService;

    // 루틴 등록
    @PostMapping
    @Operation(summary = "루틴 등록", description = "제목, 내용, 시작시각, 시작일, 종료일, 요일목록을 받아 루틴을 등록하고 60일치 일정을 생성합니다.")
    public ResponseEntity<RoutineResponseDTO> createRoutine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RoutineRequestDTO request) {

        RoutineResponseDTO response = routineService.saveRoutine(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 루틴 전체 조회
    @GetMapping
    @Operation(summary = "루틴 목록 조회", description = "로그인한 사용자가 설정해 둔 전체 루틴 목록을 조회합니다.")
    public ResponseEntity<List<RoutineResponseDTO>> getRoutines(@AuthenticationPrincipal CustomUserDetails userDetails) {

        List<RoutineResponseDTO> response = routineService.getRoutines(userDetails.userId());
        return ResponseEntity.ok(response);
    }

    // 루틴 단건 조회
    @GetMapping("/{routineId}")
    @Operation(summary = "루틴 상세 조회", description = "특정 루틴의 정보를 조회합니다.")
    public ResponseEntity<RoutineResponseDTO> getRoutine(@PathVariable Long routineId) {

        RoutineResponseDTO response = routineService.getRoutine(routineId);
        return ResponseEntity.ok(response);
    }

    // 루틴 수정
    @PutMapping("/{routineId}")
    @Operation(summary = "루틴 수정", description = "사용자가 등록한 루틴의 정보를 수정하고, 미래의 일정을 재조정합니다.")
    public ResponseEntity<RoutineResponseDTO> updateRoutine(@PathVariable Long routineId,
                                                            @Valid @RequestBody RoutineRequestDTO request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        RoutineResponseDTO response = routineService.updateRoutine(routineId, request, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    // 루틴 삭제
    @DeleteMapping("/{routineId}")
    @Operation(summary = "루틴 삭제", description = "사용자가 등록한 루틴을 삭제하고, 이로 인해 생성된 미래 일정을 모두 삭제합니다.")
    public ResponseEntity<Void> deleteRoutine(@PathVariable Long routineId,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        routineService.deleteRoutine(routineId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
