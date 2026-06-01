package learntime.backend.domain.user.converter;

import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.model.UserTerms;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import learntime.backend.domain.badge.model.UserBadge;
import learntime.backend.domain.point.enums.PointMilestone;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.user.dto.response.UserBadgeResponseDTO;
import learntime.backend.domain.user.dto.response.UserSummaryResponseDTO;
import learntime.backend.domain.study.dto.response.TodayStudyPlanResponseDTO;
import learntime.backend.domain.user.dto.response.RecentActivityResponseDTO;
import learntime.backend.domain.user.enums.RecentActivityType;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import learntime.backend.domain.user.dto.response.BadgeTierInfoResponseDTO;
import learntime.backend.domain.badge.enums.BadgeType;

public class UserConverter {

    public UserConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static MyPageResponseDTO toMyPageResponseDTO(User user) {
        return MyPageResponseDTO.builder()
                .email(user.getEmail())
                .userName(user.getName())
                .point(user.getPoint())
                .socialProvider(user.getSocialProvider().name())
                .termsAgreements(user.getUserTerms().stream()
                        .collect(Collectors.toMap(ut -> ut.getTerms().name(), UserTerms::getAgreed)))
                .createdAt(user.getCreatedAt())
                .role(user.getRole())
                .build();
    }

    public static User toUserEntity(SignUpRequestDTO signUpData, String encodedPassword) {
        return User.builder()
                .name(signUpData.userName())
                .email(signUpData.email())
                .password(encodedPassword)
                .role(Role.ROLE_USER) // 관리자는 ROLE_ADMIN
                .build();
    }

    public static UserSummaryResponseDTO toUserSummaryResponseDTO(User user, List<UserBadge> badges) {
        PointMilestone currentTier = PointMilestone.getTier(user.getPoint());
        int nextMinPoint = currentTier.getMinPoint();
        PointMilestone[] tiers = PointMilestone.values();
        for (int i = 0; i < tiers.length - 1; i++) {
            if (tiers[i] == currentTier) {
                nextMinPoint = tiers[i + 1].getMinPoint();
                break;
            }
        }

        return UserSummaryResponseDTO.builder()
                .point(user.getPoint())
                .tierName(currentTier.getTierName())
                .badges(badges.stream()
                        .map(UserConverter::toUserBadgeResponseDTO)
                        .toList())
                .nextMinPoint(nextMinPoint)
                .build();
    }


    public static BadgeTierInfoResponseDTO toBadgeTierInfoResponseDTO(User user, List<UserBadge> userBadges) {
        PointMilestone currentTier = PointMilestone.getTier(user.getPoint());

        List<BadgeTierInfoResponseDTO.TierInfoDTO> allTiers = Arrays.stream(PointMilestone.values())
                .map(tier -> BadgeTierInfoResponseDTO.TierInfoDTO.builder()
                        .tierName(tier.getTierName())
                        .minPoint(tier.getMinPoint())
                        .build())
                .toList();

        List<BadgeTierInfoResponseDTO.BadgeInfoDTO> allBadges = Arrays.stream(BadgeType.values())
                .map(badge -> BadgeTierInfoResponseDTO.BadgeInfoDTO.builder()
                        .badgeType(badge.name())
                        .displayName(badge.getDisplayName())
                        .description(badge.getDescription())
                        .build())
                .toList();

        List<UserBadgeResponseDTO> acquiredBadges = userBadges.stream()
                .map(UserConverter::toUserBadgeResponseDTO)
                .toList();

        return BadgeTierInfoResponseDTO.builder()
                .allTiers(allTiers)
                .allBadges(allBadges)
                .currentTierName(currentTier.getTierName())
                .acquiredBadges(acquiredBadges)
                .build();
    }

    public static UserBadgeResponseDTO toUserBadgeResponseDTO(UserBadge userBadge) {
        return UserBadgeResponseDTO.builder()
                .badgeType(userBadge.getBadgeType().name())
                .displayName(userBadge.getBadgeType().getDisplayName())
                .description(userBadge.getBadgeType().getDescription())
                .acquiredAt(userBadge.getAcquiredAt())
                .build();
    }

    public static TodayStudyPlanResponseDTO toTodayStudyPlanResponseDTO(StudyMember member, StudyDailyPlan plan, StudyStatus status) {
        return TodayStudyPlanResponseDTO.builder()
                .studyId(member.getStudy().getStudyId())
                .studyTitle(member.getStudy().getStudyTitle())
                .studyDailyPlanId(plan.getStudyDailyPlanId())
                .planContent(plan.getPlanContent())
                .progressStatus(status != null ? status.getProgressStatus() : ProgressStatus.NOT_STARTED)
                .build();
    }

    public static RecentActivityResponseDTO toRecentActivityResponseDTOForNote(StudyNotes note) {
        return RecentActivityResponseDTO.builder()
                .type(RecentActivityType.NOTE)
                .id(note.getStudyNotesId())
                .title(note.getNoteTitle())
                .studyTitle(note.getStudyMember().getStudy().getStudyTitle())
                .createdAt(note.getCreatedAt())
                .build();
    }

    public static RecentActivityResponseDTO toRecentActivityResponseDTOForFeedback(StudyFeedback feedback) {
        return RecentActivityResponseDTO.builder()
                .type(RecentActivityType.FEEDBACK)
                .id(feedback.getStudyFeedbackId())
                .title(feedback.getFeedbackTitle())
                .studyTitle(feedback.getStudyMember().getStudy().getStudyTitle())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    public static RecentActivityResponseDTO toRecentActivityResponseDTOForQuiz(QuizHistory quizHistory) {
        return RecentActivityResponseDTO.builder()
                .type(RecentActivityType.QUIZ)
                .id(quizHistory.getQuizHistoryId())
                .title(quizHistory.getStudyQuiz().getQuizTitle())
                .studyTitle(quizHistory.getStudyQuiz().getStudyMember().getStudy().getStudyTitle())
                .createdAt(quizHistory.getSubmittedAt())
                .build();
    }
}

