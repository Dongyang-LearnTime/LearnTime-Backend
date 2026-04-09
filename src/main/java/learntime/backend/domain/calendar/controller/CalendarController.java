package learntime.backend.domain.calendar.controller;

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
public class CalendarController {

    private final CalendarService calendarService;

    // 일정 등록
    @PostMapping
    public ResponseEntity<CalendarResponseDTO> createSchedule(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CalendarRequestDTO request) {

        // CustomUserDetails에서 이메일(Username)을 꺼내 서비스에 전달합니다. [cite: 74]
        CalendarResponseDTO response = calendarService.saveSchedule(user.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 일정 조회
    @GetMapping
    public ResponseEntity<List<CalendarResponseDTO>> getMonthlySchedules(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month) {

        List<CalendarResponseDTO> response = calendarService.getMonthlySchedules(user.getUsername(), year, month);
        return ResponseEntity.ok(response);
    }

    // 일정 수정
    @PutMapping("/{id}")
    public ResponseEntity<CalendarResponseDTO> updateSchedule(
            @PathVariable(name = "id") Long calendarRecordId,
            @Valid @RequestBody CalendarRequestDTO request) {

        CalendarResponseDTO response = calendarService.updateSchedule(calendarRecordId, request);
        return ResponseEntity.ok(response);
    }

    // 일정 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable(name = "id") Long calendarRecordId) {
        calendarService.deleteSchedule(calendarRecordId);
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}