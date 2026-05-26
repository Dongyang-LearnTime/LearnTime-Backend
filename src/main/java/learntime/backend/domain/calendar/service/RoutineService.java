package learntime.backend.domain.calendar.service;

import learntime.backend.domain.calendar.converter.CalenderConverter;
import learntime.backend.domain.calendar.dto.request.RoutineRequestDTO;
import learntime.backend.domain.calendar.dto.response.RoutineResponseDTO;
import learntime.backend.domain.calendar.error.code.CalenderErrorCode;
import learntime.backend.domain.calendar.error.exception.CalenderException;
import learntime.backend.domain.calendar.event.CalendarReminderDeleteEvent;
import learntime.backend.domain.calendar.event.CalendarReminderUpsertEvent;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.calendar.model.Routine;
import learntime.backend.domain.calendar.repository.CalendarRecordRepository;
import learntime.backend.domain.calendar.repository.RoutineRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final CalendarRecordRepository calendarRecordRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int GENERATION_DAYS = 60;

    // 루틴 등록
    @Transactional
    public RoutineResponseDTO saveRoutine(Long userId, RoutineRequestDTO request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new CalenderException(CalenderErrorCode.INVALID_DATE_RANGE);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Routine routine = CalenderConverter.toRoutine(request, user);
        Routine savedRoutine = routineRepository.save(routine);

        // 첫 60일치 일정 생성
        generateCalendarRecordsForRoutine(savedRoutine, request.startDate(), GENERATION_DAYS);
        return CalenderConverter.toRoutineResponseDTO(savedRoutine);
    }

    // 루틴 조회 (특정 사용자의 전체 루틴 목록)
    @Transactional(readOnly = true)
    public List<RoutineResponseDTO> getRoutines(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        return routineRepository.findAllByUser(user).stream()
                .map(CalenderConverter::toRoutineResponseDTO)
                .collect(Collectors.toList());
    }

    // 루틴 상세 조회
    @Transactional(readOnly = true)
    public RoutineResponseDTO getRoutine(Long routineId) {
        Routine routine = getRoutineOrThrow(routineId);

        return CalenderConverter.toRoutineResponseDTO(routine);
    }

    // 루틴 수정
    @Transactional
    public RoutineResponseDTO updateRoutine(Long routineId, RoutineRequestDTO request, Long userId) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new CalenderException(CalenderErrorCode.INVALID_DATE_RANGE);
        }
        Routine routine = getRoutineOrThrow(routineId);

        AuthorizationUtil.verifyOwnership(userId, routine.getUser().getUserId());

        LocalDateTime now = LocalDateTime.now();
        List<CalendarRecord> futureRecords = calendarRecordRepository.findAllByRoutineAndTargetDateAfter(routine, now);

        for (CalendarRecord record : futureRecords) {
            eventPublisher.publishEvent(new CalendarReminderDeleteEvent(record));
        }
        calendarRecordRepository.deleteByRoutineAndTargetDateAfter(routine, now);

        // 루틴 정보 업데이트
        routine.update(
                request.content(),
                request.startTime(),
                request.startDate(),
                request.endDate(),
                request.isImportant() != null && request.isImportant(),
                request.daysOfWeek()
        );

        // 수정된 정보 기준 오늘 이후로 60일치 일정 재생성
        LocalDate generateStart = request.startDate().isAfter(LocalDate.now()) ? request.startDate() : LocalDate.now();
        generateCalendarRecordsForRoutine(routine, generateStart, GENERATION_DAYS);

        return CalenderConverter.toRoutineResponseDTO(routine);
    }

    // 루틴 삭제
    @Transactional
    public void deleteRoutine(Long routineId, Long userId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new CalenderException(CalenderErrorCode.CALENDAR_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, routine.getUser().getUserId());

        // 미래의 생성된 일정들 삭제 및 알림 이벤트 취소
        LocalDateTime now = LocalDateTime.now();
        List<CalendarRecord> futureRecords = calendarRecordRepository.findAllByRoutineAndTargetDateAfter(routine, now);

        for (CalendarRecord record : futureRecords) {
            eventPublisher.publishEvent(new CalendarReminderDeleteEvent(record));
        }
        calendarRecordRepository.deleteByRoutineAndTargetDateAfter(routine, now);

        // 루틴 삭제 (CascadeType.ALL에 의해 과거 일정도 함께 지워짐)
        routineRepository.delete(routine);
    }

    private Routine getRoutineOrThrow(Long routineId) {
        return routineRepository.findById(routineId)
                .orElseThrow(() -> new CalenderException(CalenderErrorCode.CALENDAR_NOT_FOUND));
    }

    // 루틴 기반 실제 일정 생성 헬퍼
    @Transactional
    public void generateCalendarRecordsForRoutine(Routine routine, LocalDate startFrom, int daysToGenerate) {
        List<CalendarRecord> newRecords = new ArrayList<>();
        LocalDate endDateLimit = routine.getEndDate();

        for (int i = 0; i < daysToGenerate; i++) {
            LocalDate date = startFrom.plusDays(i);

            // 종료일 조건 체크
            if (endDateLimit != null && date.isAfter(endDateLimit)) {
                break;
            }

            // 지정 요일과 일치하는지 체크
            if (routine.getDaysOfWeek().contains(date.getDayOfWeek())) {
                LocalDateTime targetDate = LocalDateTime.of(date, routine.getStartTime());

                CalendarRecord record = CalendarRecord.builder()
                        .user(routine.getUser())
                        .content(routine.getContent())
                        .targetDate(targetDate)
                        .isImportant(routine.getIsImportant())
                        .routine(routine)
                        .build();

                newRecords.add(record);
            }
        }

        if (!newRecords.isEmpty()) {
            List<CalendarRecord> savedRecords = calendarRecordRepository.saveAll(newRecords);
            // 리마인더 알림 예약 이벤트 발행
            for (CalendarRecord record : savedRecords) {
                eventPublisher.publishEvent(new CalendarReminderUpsertEvent(record));
            }
            log.info("루틴(ID: {})에 대한 일정 {}건이 생성되었습니다.", routine.getRoutineId(), savedRecords.size());
        }
    }
}
