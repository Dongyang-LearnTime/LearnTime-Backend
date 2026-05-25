package learntime.backend.domain.calendar.service;

import learntime.backend.domain.calendar.converter.CalenderConverter;
import learntime.backend.domain.calendar.dto.request.CalendarRequestDTO;
import learntime.backend.domain.calendar.dto.response.CalendarResponseDTO;
import learntime.backend.domain.calendar.error.code.CalenderErrorCode;
import learntime.backend.domain.calendar.error.exception.CalenderException;
import learntime.backend.domain.calendar.event.CalendarReminderDeleteEvent;
import learntime.backend.domain.calendar.event.CalendarReminderUpsertEvent;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.calendar.repository.CalendarRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
// 캘린더 일정 CRUD와 리마인더 예약 이벤트 발행을 담당하는 Service
public class CalendarService {
    private final CalendarRecordRepository calendarRecordRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 일정 등록
    @Transactional
    public CalendarResponseDTO saveSchedule(Long userId, CalendarRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        CalendarRecord record = CalenderConverter.toCalendarRecord(request, user);

        CalendarRecord saved = calendarRecordRepository.save(record);

        // 리마인더 예약은 이벤트 리스너가 같은 트랜잭션 커밋 직전에 처리
        eventPublisher.publishEvent(new CalendarReminderUpsertEvent(saved));
        return CalenderConverter.toCalendarResponseDTO(saved);
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
                .map(CalenderConverter::toCalendarResponseDTO)
                .collect(Collectors.toList());
    }

    // 일정 수정
    @Transactional
    public CalendarResponseDTO updateSchedule(Long calendarRecordId, CalendarRequestDTO request, Long userId) {
        CalendarRecord record = calendarRecordRepository.findById(calendarRecordId)
                .orElseThrow(() -> new CalenderException(CalenderErrorCode.CALENDAR_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, record.getUser().getUserId());

        record.update(request.content(),
                request.targetDate(),
                request.isCompleted(),
                request.isImportant() != null && request.isImportant());

        // 리마인더 재예약은 이벤트 리스너가 같은 트랜잭션 커밋 직전에 처리
        eventPublisher.publishEvent(new CalendarReminderUpsertEvent(record));

        return CalenderConverter.toCalendarResponseDTO(record);
    }

    // 일정 삭제
    @Transactional
    public void deleteSchedule(Long calendarRecordId, Long userId) {
        CalendarRecord record = calendarRecordRepository.findById(calendarRecordId)
                .orElseThrow(() -> new CalenderException(CalenderErrorCode.CALENDAR_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, record.getUser().getUserId());

        // 리마인더 삭제는 이벤트 리스너가 캘린더 삭제 커밋 직전에 처리
        eventPublisher.publishEvent(new CalendarReminderDeleteEvent(record));

        calendarRecordRepository.delete(record);
    }
}
