package learntime.backend.domain.user.service;

import learntime.backend.domain.calendar.repository.CalendarRecordRepository;
import learntime.backend.domain.calendar.repository.RoutineRepository;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostLikeRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.exercise.repository.MealRecordRepository;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.notification.repository.NotificationRepository;
import learntime.backend.domain.notification.repository.ReminderRepository;
import learntime.backend.domain.study_member.repository.StudyInvitationRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.friend.repository.FriendRepository;
import learntime.backend.domain.friend.repository.FriendRequestRepository;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.domain.message.repository.MessageRepository;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.badge.repository.UserBadgeRepository;
import learntime.backend.domain.badge.repository.UserActivityStatRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import learntime.backend.domain.badge.model.UserBadge;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.notes.repository.StudyNotesRepository;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.repository.StudyFeedbackRepository;
import learntime.backend.domain.user.converter.UserConverter;
import learntime.backend.domain.user.dto.response.RecentActivityResponseDTO;
import learntime.backend.domain.user.dto.response.UserSummaryResponseDTO;
import learntime.backend.global.dto.CursorResponse;

import java.util.ArrayList;
import java.util.Comparator;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PromptQuotaRepository promptQuotaRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final MealRecordRepository mealRecordRepository;
    private final ReminderRepository reminderRepository;
    private final CalendarRecordRepository calendarRecordRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final NotificationRepository notificationRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyInvitationRepository studyInvitationRepository;
    private final UserTermsRepository userTermsRepository;
    private final MessageRepository messageRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityStatRepository userActivityStatRepository;
    private final StudyNotesRepository studyNotesRepository;
    private final StudyFeedbackRepository studyFeedbackRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final ProfileRepository profileRepository;
    private final RoutineRepository routineRepository;

    private static final DateTimeFormatter DELETED_USER_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 이름 중복 체크
    @Transactional(readOnly = true)
    public boolean isNameDuplicated(String name) {
        return userRepository.existsByName(name);
    }

    // 이메일 중복 체크
    @Transactional(readOnly = true)
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }

    // 이름(닉네임)으로 사용자 ID 목록 검색 (커서 기반 페이징)
    @Transactional(readOnly = true)
    public CursorResponse<Long> searchUserIdsByName(String keyword, Long lastUserId, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return CursorResponse.of(List.of(), null, false);
        }
        
        Pageable limit = PageRequest.of(0, size + 1);
        List<Long> userIds;
        if (lastUserId == null) {
            userIds = userRepository.findUserIdsByNameContaining(keyword.trim(), limit);
        } else {
            userIds = userRepository.findUserIdsByNameContainingWithCursor(keyword.trim(), lastUserId, limit);
        }
        
        boolean hasNext = userIds.size() > size;
        List<Long> content = hasNext ? userIds.subList(0, size) : userIds;
        Long nextCursor = null;
        if (!content.isEmpty()) {
            nextCursor = content.get(content.size() - 1);
        }
        
        return CursorResponse.of(content, hasNext ? nextCursor : null, hasNext);
    }


    // 사용자 뱃지, 티어
    @Transactional(readOnly = true)
    public UserSummaryResponseDTO getUserSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<UserBadge> badges = userBadgeRepository.findAllByUserId(user.getUserId());

        return UserConverter.toUserSummaryResponseDTO(user, badges);
    }

    // 전체 티어, 뱃지 정보 및 사용자의 취득 상태
    @Transactional(readOnly = true)
    public learntime.backend.domain.user.dto.response.BadgeTierInfoResponseDTO getBadgeTierInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<UserBadge> badges = userBadgeRepository.findAllByUserId(user.getUserId());

        return UserConverter.toBadgeTierInfoResponseDTO(user, badges);
    }

    // 사용자의 최근 필기, 퀴즈, AI 답변 중 최신순 3개 가져옴
    @Transactional(readOnly = true)
    public List<RecentActivityResponseDTO> getRecentActivities(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Pageable limitThree = PageRequest.of(0, 3);
        List<StudyNotes> notes = studyNotesRepository.findTop3ByUserId(user.getUserId(), limitThree);
        List<StudyFeedback> feedbacks = studyFeedbackRepository.findTop3ByUserId(user.getUserId(), limitThree);
        List<QuizHistory> quizzes = quizHistoryRepository.findTop3ByUserId(user.getUserId(), limitThree);

        List<RecentActivityResponseDTO> mergedActivities = new ArrayList<>();
        notes.forEach(note -> mergedActivities.add(UserConverter.toRecentActivityResponseDTOForNote(note)));
        feedbacks.forEach(feedback -> mergedActivities.add(UserConverter.toRecentActivityResponseDTOForFeedback(feedback)));
        quizzes.forEach(quiz -> mergedActivities.add(UserConverter.toRecentActivityResponseDTOForQuiz(quiz)));

        return mergedActivities.stream()
                .sorted(Comparator.comparing(RecentActivityResponseDTO::createdAt).reversed())
                .limit(3)
                .toList();
    }

    // 회원 탈퇴 로직
    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        Long userId = user.getUserId();
        LocalDateTime deletedAt = LocalDateTime.now();

        // 탈퇴 시 토큰/할당량/개인 기록을 먼저 정리한다.
        refreshTokenRepository.deleteByUser(user);
        promptQuotaRepository.deleteByUserId(userId);

        exerciseRecordRepository.deleteBodyPartsByUserId(userId);
        exerciseRecordRepository.deleteAllByUserId(userId);
        weightRecordRepository.deleteAllByUserId(userId);
        mealRecordRepository.deleteAllByUserId(userId);

        reminderRepository.deleteAllByCalendarUserId(userId);
        calendarRecordRepository.deleteAllByUserId(userId);
        routineRepository.deleteDaysByUserId(userId);
        routineRepository.deleteAllByUserId(userId);

        postRepository.decrementLikeCountForUserLikes(userId);
        postLikeRepository.deleteAllByUserId(userId);
        postRepository.detachAuthorByUserId(userId);
        commentRepository.detachAuthorByUserId(userId);

        friendRequestRepository.deleteAllByUserId(userId);
        friendRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByReceiverId(userId);
        messageRepository.deleteSentMessagesByUserId(userId);
        messageRepository.deleteReceivedMessagesByUserId(userId);
        studyInvitationRepository.cancelPendingByUserId(userId, deletedAt);
        reassignOwnedStudies(userId);
        studyMemberRepository.withdrawAllByUserId(userId);
        userTermsRepository.deleteAllByUserId(userId);
        userBadgeRepository.deleteAllByUserId(userId);
        userActivityStatRepository.deleteAllByUserId(userId);
        profileRepository.deleteByUser_UserId(userId);

        userRepository.anonymizeAndSoftDelete(
                userId,
                createDeletedEmail(userId, deletedAt),
                createDeletedName(userId),
                deletedAt
        );
    }

    private String createDeletedEmail(Long userId, LocalDateTime deletedAt) {
        return "deleted_%d_%s@deleted.local".formatted(
                userId,
                deletedAt.format(DELETED_USER_TIMESTAMP_FORMATTER)
        );
    }

    private String createDeletedName(Long userId) {
        return "탈퇴한 사용자_%d".formatted(userId);
    }

    private void reassignOwnedStudies(Long userId) {
        List<StudyMember> ownedMemberships = studyMemberRepository.findOwnedMemberships(
                userId,
                StudyMemberRole.OWNER,
                StudyMemberStatus.ACTIVE
        );

        for (StudyMember ownedMembership : ownedMemberships) {
            List<StudyMember> activeMembers = studyMemberRepository.findAllByStudy_StudyIdAndStatus(
                    ownedMembership.getStudy().getStudyId(),
                    StudyMemberStatus.ACTIVE
            );

            List<StudyMember> candidates = activeMembers.stream()
                    .filter(member -> !member.getUser().getUserId().equals(userId))
                    .toList();

            if (candidates.isEmpty()) {
                continue;
            }

            StudyMember promotedMember = candidates.get(
                    ThreadLocalRandom.current().nextInt(candidates.size())
            );
            promotedMember.changeRole(StudyMemberRole.OWNER);
            ownedMembership.changeRole(StudyMemberRole.MEMBER);
        }
    }


}
