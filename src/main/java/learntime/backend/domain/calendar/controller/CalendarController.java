package learntime.backend.domain.calendar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import learntime.backend.domain.calendar.dto.request.CalendarRequestDTO;
import learntime.backend.domain.calendar.dto.response.CalendarResponseDTO;
import learntime.backend.domain.calendar.service.CalendarService;
import learntime.backend.global.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/calendar")
@RequiredArgsConstructor
@Tag(name = "캘린더 API", description = "일정 관련 CRUD API (JWT 필요)")
public class CalendarController {

    private final CalendarService calendarService;

    // 일정 등록
    @PostMapping
    @Operation(summary = "일정 등록", description = "제목, 내용, 목표시간을 설정 후 일정을 등록합니다.")
    public ResponseEntity<CalendarResponseDTO> createSchedule(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CalendarRequestDTO request) {

        CalendarResponseDTO response = calendarService.saveSchedule(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 일정 조회
    @GetMapping
    @Operation(summary = "일정 조회", description = "등록한 일정 정보를 조회합니다.")
    public ResponseEntity<List<CalendarResponseDTO>> getMonthlySchedules(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month) {

        List<CalendarResponseDTO> response = calendarService.getMonthlySchedules(user.userId(), year, month);
        return ResponseEntity.ok(response);
    }

    // 일정 수정
    @PutMapping("/{id}")
    @Operation(summary = "일정 수정", description = "사용자가 등록한 일정 정보를 수정합니다.")
    public ResponseEntity<CalendarResponseDTO> updateSchedule(
            @PathVariable(name = "id") Long calendarRecordId,
            @Valid @RequestBody CalendarRequestDTO request) {

        CalendarResponseDTO response = calendarService.updateSchedule(calendarRecordId, request);
        return ResponseEntity.ok(response);
    }

    // 일정 삭제
    @DeleteMapping("/{id}")
    @Operation(summary = "일정 삭제", description = "사용자가 등록한 일정을 삭제합니다.")
    public ResponseEntity<Void> deleteSchedule(@PathVariable(name = "id") Long calendarRecordId) {
        calendarService.deleteSchedule(calendarRecordId);
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}
