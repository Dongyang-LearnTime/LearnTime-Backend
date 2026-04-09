package learntime.backend.domain.calendar.service;

import learntime.backend.domain.calendar.dto.request.CalendarRequestDTO;
import learntime.backend.domain.calendar.dto.response.CalendarResponseDTO;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.calendar.repository.CalendarRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.error.code.ErrorCode;
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

    // 일정 등록
    @Transactional
    public CalendarResponseDTO saveSchedule(String email, CalendarRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        CalendarRecord record = CalendarRecord.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .targetDate(request.targetDate())
                .isCompleted(request.isCompleted() != null && request.isCompleted())
                .build();

        CalendarRecord saved = calendarRecordRepository.save(record);
        log.info("새로운 일정이 등록되었습니다: ID={}, 제목={}", saved.getCalendarRecordId(), saved.getTitle());

        return CalendarResponseDTO.from(saved);
    }

    // 월별 일정 조회
    @Transactional(readOnly = true)
    public List<CalendarResponseDTO> getMonthlySchedules(String email, int year, int month) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));


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
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        record.update(
                request.title(),
                request.content(),
                request.targetDate(),
                request.isCompleted()
        );

        return CalendarResponseDTO.from(record);
    }

    // 일정 삭제
    @Transactional
    public void deleteSchedule(Long calendarRecordId) {
        if (!calendarRecordRepository.existsById(calendarRecordId)) {
            throw new BusinessException(ErrorCode.CALENDAR_NOT_FOUND);
        }
        calendarRecordRepository.deleteById(calendarRecordId);
        log.info("일정이 삭제되었습니다: ID={}", calendarRecordId);
    }

}
