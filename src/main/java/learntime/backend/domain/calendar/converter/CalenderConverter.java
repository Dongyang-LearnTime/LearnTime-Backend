package learntime.backend.domain.calendar.converter;

import learntime.backend.domain.calendar.dto.request.CalendarRequestDTO;
import learntime.backend.domain.calendar.dto.request.RoutineRequestDTO;
import learntime.backend.domain.calendar.dto.response.CalendarResponseDTO;
import learntime.backend.domain.calendar.dto.response.RoutineResponseDTO;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.calendar.model.Routine;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class CalenderConverter {

    public CalenderConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static CalendarRecord toCalendarRecord(CalendarRequestDTO request, User user) {
        boolean isImportant = Boolean.TRUE.equals(request.isImportant());

        return CalendarRecord.builder()
                .user(user)
                .content(request.content())
                .targetDate(request.targetDate())
                .isImportant(isImportant)
                .build();
    }

    public static CalendarResponseDTO toCalendarResponseDTO(CalendarRecord record) {
        return CalendarResponseDTO.builder()
                .calendarRecordId(record.getCalendarRecordId())
                .content(record.getContent())
                .targetDate(record.getTargetDate())
                .isImportant(record.getIsImportant())
                .createdAt(record.getCreatedAt())
                .build();
    }

    public static Routine toRoutine(RoutineRequestDTO request, User user) {
        boolean isImportant = Boolean.TRUE.equals(request.isImportant());

        return Routine.builder()
                .user(user)
                .content(request.content())
                .startTime(request.startTime())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isImportant(isImportant)
                .daysOfWeek(request.daysOfWeek())
                .build();
    }

    public static RoutineResponseDTO toRoutineResponseDTO(Routine routine) {
        return RoutineResponseDTO.builder()
                .routineId(routine.getRoutineId())
                .content(routine.getContent())
                .startTime(routine.getStartTime())
                .startDate(routine.getStartDate())
                .endDate(routine.getEndDate())
                .isImportant(routine.getIsImportant())
                .daysOfWeek(routine.getDaysOfWeek())
                .createdAt(routine.getCreatedAt())
                .build();
    }

}
