package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study_plan.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.converter.StudyMemberConverter;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.event.StudyInvitationSentEvent;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.repository.StudyInvitationRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_plan.service.core.StudyRestManager;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyInitializationService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyInvitationRepository studyInvitationRepository;
    private final UserRepository userRepository;
    private final StudyRestManager studyRestManager;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 초기 상태(PLANNING)로 스터디 정보를 먼저 저장함 (비동기 처리 준비)
     */
    @Transactional
    public Long initializeStudy(GeminiStudyRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        try {
            Study study = StudyConverter.toStudyEntity(request, user);
            studyRepository.save(study);

            StudyMember owner = StudyMember.builder()
                    .user(user)
                    .study(study)
                    .studyMemberRole(StudyMemberRole.OWNER)
                    .build();

            studyMemberRepository.save(owner);

            if (request.studyMemberList() != null && !request.studyMemberList().isEmpty()) {
                List<User> additionalUsers = userRepository.findAllById(request.studyMemberList());
                List<StudyInvitation> invitations = new ArrayList<>();
                for (User invitedUser : additionalUsers) {
                    if (invitedUser.getUserId().equals(userId)) {
                        continue;
                    }

                    invitations.add(StudyMemberConverter.toStudyInvitation(study, invitedUser, user));
                }

                List<StudyInvitation> savedInvitations = studyInvitationRepository.saveAll(invitations);
                for (StudyInvitation savedInvitation : savedInvitations) {
                    eventPublisher.publishEvent(new StudyInvitationSentEvent(
                            savedInvitation.getStudyInvitationId(),
                            study.getStudyId(),
                            study.getStudyTitle(),
                            user.getName(),
                            savedInvitation.getInvitedUser().getUserId()
                    ));
                }
            }

            studyRestManager.saveRestDates(study, request.restDates());
            studyRestManager.saveRestDays(study, request.getRestDaysAsDayOfWeek());

            return study.getStudyId();
        } catch (Exception e) {
            log.error("스터디 초기화 실패", e);
            throw new StudyException(StudyErrorCode.STUDY_SAVE_FAILED);
        }
    }
}
