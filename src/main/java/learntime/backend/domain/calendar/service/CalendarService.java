package learntime.backend.domain.calendar.service;

import learntime.backend.domain.calendar.dto.request.CalendarRequestDTO;
import learntime.backend.domain.calendar.dto.response.CalendarResponseDTO;
import learntime.backend.domain.calendar.error.code.CalenderErrorCode;
import learntime.backend.domain.calendar.error.exception.CalenderException;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.calendar.repository.CalendarRecordRepository;
import learntime.backend.domain.notification.service.ReminderService;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {
    private final CalendarRecordRepository calendarRecordRepository;
    private final UserRepository userRepository;
    private final ReminderService reminderService;

    // 일정 등록
    @Transactional
    public CalendarResponseDTO saveSchedule(Long userId, CalendarRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        CalendarRecord record = CalendarRecord.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .targetDate(request.targetDate())
                .isCompleted(request.isCompleted() != null && request.isCompleted())
                .build();

        CalendarRecord saved = calendarRecordRepository.save(record);

        // 리마인더(알림) 생성
        reminderService.upsertReminder(saved, request.reminderOption());
        return CalendarResponseDTO.from(saved);
    }

    // 월별 일정 조회
    @Transactional(readOnly = true)
    public List<CalendarResponseDTO> getMonthlySchedules(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 해당 월의 시작일과 종료일 계산
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

        List<CalendarRecord> records = calendarRecordRepository
                .findAllByUserAndTargetDateBetweenOrderByTargetDateAsc(user, startOfMonth, endOfMonth);

        return records.stream()
                .map(CalendarResponseDTO::from)
                .collect(Collectors.toList());
    }

    // 일정 수정
    @Transactional
    public CalendarResponseDTO updateSchedule(Long calendarRecordId, CalendarRequestDTO request) {
        CalendarRecord record = calendarRecordRepository.findById(calendarRecordId)
                .orElseThrow(() -> new CalenderException(CalenderErrorCode.CALENDAR_NOT_FOUND));

        record.update(request.title(), request.content(), request.targetDate(), request.isCompleted());

        // 리마인더 업데이트 로직 연결 (시간이 바뀌면 알림 시간도 재계산됨)
        reminderService.upsertReminder(record, request.reminderOption());

        return CalendarResponseDTO.from(record);
    }

    // 일정 삭제
    @Transactional
    public void deleteSchedule(Long calendarRecordId) {
        CalendarRecord record = calendarRecordRepository.findById(calendarRecordId)
                .orElseThrow(() -> new CalenderException(CalenderErrorCode.CALENDAR_NOT_FOUND));

        // 리마인더 먼저 삭제
        reminderService.deleteReminder(record);

        calendarRecordRepository.delete(record);
    }
}
