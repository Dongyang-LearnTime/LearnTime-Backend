package learntime.backend.domain.quiz.service;

import learntime.backend.domain.quiz.dto.request.QuizSolveRequestDTO;
import learntime.backend.domain.quiz.dto.request.UpdateQuizTitleRequestDTO;
import learntime.backend.domain.quiz.dto.response.*;
import learntime.backend.domain.quiz.converter.StudyQuizConverter;
import learntime.backend.domain.quiz.dto.request.QuizCreateRequestDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.quiz.model.StudyQuiz;
import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import learntime.backend.domain.notes.repository.StudyNotesRepository;
import learntime.backend.domain.quiz.repository.StudyQuizRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.dto.PageResponse;
import learntime.backend.global.utils.StudyAuthUtil;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyQuizFacade {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyQuizRepository studyQuizRepository;
    private final StudyNotesRepository studyNotesRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final GeminiQuizService geminiQuizService;
    private final StudyQuizService studyQuizService;
    private final PromptQuotaUtil promptQuotaUtil;

    private static final int OX_COUNT = 2; // OX 퀴즈 문제 개수
    private static final int MULTIPLE_COUNT = 2; // 4지선다 문제 개수

    @Transactional(readOnly = true)
    public PageResponse<StudyQuizInfoResponseDTO> getStudyQuizList(Long studyId, Pageable pageable, Long userId) {
        // 탈퇴(WITHDRAWN) 멤버도 자신의 퀴즈 목록을 조회할 수 있음
        StudyMember studyMember = findByStudyIdAndUserIdAllowWithdrawn(studyId, userId);
        StudyAuthUtil.verifyStudyMemberAllowWithdrawn(studyId, userId, studyMemberRepository);

        return studyQuizService.getStudyQuizList(studyMember.getStudyMemberId(), pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizHistoryInfoResponseDTO> getQuizHistoryList(Long studyQuizId, Pageable pageable, Long userId) {
        StudyQuiz studyQuiz = studyQuizRepository.findById(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));
        // 본인만 조회 가능
        StudyAuthUtil.verifyOwnership(studyQuiz.getStudyMember(), userId);

        return studyQuizService.getQuizHistoryList(studyQuizId, pageable);
    }

    @Transactional(readOnly = true)
    public StudyQuizResponseDTO getStudyQuizWithQuestions(Long studyQuizId, Long userId) {
        // 퀴즈, 퀴즈 문항 정보 같이 가져옴
        StudyQuiz studyQuiz = studyQuizRepository.findByIdWithQuestions(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));

        // 본인만 퀴즈 문항 열람 가능
        StudyAuthUtil.verifyOwnership(studyQuiz.getStudyMember(), userId);

        return StudyQuizConverter.toResponseDTO(studyQuiz, studyQuiz.getQuestions());
    }

    @Transactional(readOnly = true)
    public StudyQuizResultResponseDTO getQuizResult(Long quizHistoryId, Long userId) {
        QuizHistory quizHistory = quizHistoryRepository.findById(quizHistoryId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_HISTORY_NOT_FOUND));

        // 본인만 조회 가능
        StudyAuthUtil.verifyOwnership(quizHistory.getStudyQuiz().getStudyMember(), userId);

        return studyQuizService.getQuizResult(quizHistoryId);
    }

    // 필기를 기반으로 퀴즈 추출
    public Long generateAndSaveStudyQuiz(QuizCreateRequestDTO request, Long userId) {
        StudyMember studyMember = findByStudyIdAndUserId(request.studyId(), userId);

        // 본인만 생성 가능
        StudyAuthUtil.verifyOwnership(studyMember, userId);

        // 노트 정보 가져옴
        StudyNotes studyNotes = studyNotesRepository.findById(request.studyNotesId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));

        promptQuotaUtil.decreasePromptQuota(userId); // 프롬프트 할당량 차감

        try {
            String cleanedText = preprocessNoteContent(studyNotes.getNoteContents());
            int quizTotalCount = OX_COUNT + MULTIPLE_COUNT; // 총 문제 개수 (2+2)

            List<QuizQuestionResponseDTO> questionDos = geminiQuizService.generateQuizQuestions(
                    quizTotalCount, OX_COUNT, MULTIPLE_COUNT, cleanedText
            ); // DTO로 파싱된 AI 응답

            return studyQuizService.saveStudyQuiz(studyMember, questionDos); // DB에 퀴즈 저장 및 퀴즈 ID return
        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId); // 예외 일어나면 할당 되돌려줌
            throw e;
        }
    }

    public Long solveStudyQuiz(List<QuizSolveRequestDTO> requests, Long userId) {
        return studyQuizService.solveQuiz(requests, userId);
    }

    @Transactional
    public void updateTitle(UpdateQuizTitleRequestDTO request, Long userId) {
        StudyQuiz studyQuiz = findByStudyQuizId(request.studyQuizId());
        // 본인만 수정 가능
        StudyAuthUtil.verifyOwnership(studyQuiz.getStudyMember(), userId);

        studyQuiz.setTitle(request.quizTitle());
    }

    // 퀴즈 삭제
    @Transactional
    public void deleteStudyQuiz(Long studyQuizId, Long userId) {
        StudyQuiz studyQuiz = findByStudyQuizId(studyQuizId);
        // 본인만 삭제 가능
        StudyAuthUtil.verifyOwnership(studyQuiz.getStudyMember(), userId);

        studyQuizRepository.deleteById(studyQuizId);
    }

    // 퀴즈 이력 삭제
    @Transactional
    public void deleteQuizHistory(Long quizHistoryId, Long userId) {
        QuizHistory quizHistory = quizHistoryRepository.findById(quizHistoryId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_HISTORY_NOT_FOUND));

        // 본인만 삭제 가능
        StudyAuthUtil.verifyOwnership(quizHistory.getStudyQuiz().getStudyMember(), userId);

        quizHistoryRepository.deleteById(quizHistoryId);
    }

    /** ACTIVE 멤버 전용 조회 (퀴즈 생성 등 쓰기 작업용) */
    private StudyMember findByStudyIdAndUserId(Long studyId, Long userId) {
        return studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    /** ACTIVE + WITHDRAWN 모두 허용 (퀴즈 목록 조회용) */
    private StudyMember findByStudyIdAndUserIdAllowWithdrawn(Long studyId, Long userId) {
        return studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        studyId,
                        userId,
                        List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    private StudyQuiz findByStudyQuizId(Long studyQuizId) {
        return studyQuizRepository.findById(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));
    }

    // 필기 내용에서 HTML 태그를 뺀 문장만 추출
    private String preprocessNoteContent(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) return "";
        Document doc = Jsoup.parse(htmlContent);
        doc.select("s, strike, del").remove(); // 취소줄 등은 제외
        return doc.text();
    }

}
