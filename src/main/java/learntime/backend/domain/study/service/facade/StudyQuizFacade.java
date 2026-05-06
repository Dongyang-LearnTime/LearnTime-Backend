package learntime.backend.domain.study.service.facade;

import jakarta.validation.Valid;
import learntime.backend.domain.study.converter.StudyQuizConverter;
import learntime.backend.domain.study.dto.request.QuizCreateRequestDTO;
import learntime.backend.domain.study.dto.request.QuizSolveRequestDTO;
import learntime.backend.domain.study.dto.request.UpdateQuizTitleRequestDTO;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.QuizHistory;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyNotes;
import learntime.backend.domain.study.model.StudyQuiz;
import learntime.backend.domain.study.repository.QuizHistoryRepository;
import learntime.backend.domain.study.repository.StudyNotesRepository;
import learntime.backend.domain.study.repository.StudyQuizRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.ai.GeminiQuizService;
import learntime.backend.domain.study.service.core.StudyQuizService;
import learntime.backend.global.utils.AuthorizationUtil;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyQuizFacade {

    private final StudyRepository studyRepository;
    private final StudyQuizRepository studyQuizRepository;
    private final StudyNotesRepository studyNotesRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final GeminiQuizService geminiQuizService;
    private final StudyQuizService studyQuizService;
    private final PromptQuotaUtil promptQuotaUtil;

    private static final int OX_COUNT = 2; // OX 퀴즈 문제 개수
    private static final int MULTIPLE_COUNT = 2; // 4지선다 문제 개수

    @Transactional(readOnly = true)
    public StudyQuizListResponseDTO getStudyQuizList(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());
        return studyQuizService.getStudyQuizList(studyId);
    }

    @Transactional(readOnly = true)
    public QuizHistoryListResponseDTO getQuizHistoryList(Long studyQuizId, Long userId) {
        StudyQuiz studyQuiz = studyQuizRepository.findById(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));
        AuthorizationUtil.verifyOwnership(userId, studyQuiz.getStudy().getUser().getUserId());
        return studyQuizService.getQuizHistoryList(studyQuizId);
    }

    @Transactional(readOnly = true)
    public StudyQuizResponseDTO getStudyQuizWithQuestions(Long studyQuizId, Long userId) {
        // 퀴즈, 퀴즈 문항 정보 같이 가져옴
        StudyQuiz studyQuiz = studyQuizRepository.findByIdWithQuestions(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyQuiz.getStudy().getUser().getUserId());

        return StudyQuizConverter.toResponseDTO(studyQuiz, studyQuiz.getQuestions());
    }

    @Transactional(readOnly = true)
    public StudyQuizResultResponseDTO getQuizResult(Long studyQuizId, Long userId) {
        StudyQuiz studyQuiz = studyQuizRepository.findById(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));
        AuthorizationUtil.verifyOwnership(userId, studyQuiz.getStudy().getUser().getUserId());
        return studyQuizService.getQuizResult(studyQuizId);
    }

    // 필기를 기반으로 퀴즈 추출
    public Long generateAndSaveStudyQuiz(QuizCreateRequestDTO request, Long userId) {
        Study study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        StudyNotes studyNotes = studyNotesRepository.findById(request.studyNotesId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyNotes.getStudy().getUser().getUserId());

        promptQuotaUtil.decreasePromptQuota(userId); // 프롬프트 할당량 차감

        try {
            String cleanedText = preprocessNoteContent(studyNotes.getNoteContents());
            int quizTotalCount = OX_COUNT + MULTIPLE_COUNT; // 총 문제 개수

            List<QuizQuestionResponseDTO> questionDos = geminiQuizService.generateQuizQuestions(
                    quizTotalCount, OX_COUNT, MULTIPLE_COUNT, cleanedText
            ); // DTO로 파싱된 AI 응답

            return studyQuizService.saveStudyQuiz(study, questionDos);
        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId); // 예외 일어나면 할당 되돌려줌
            throw e;
        }
    }

    public void solveStudyQuiz(List<QuizSolveRequestDTO> requests, Long userId) {
        studyQuizService.solveQuiz(requests, userId);
    }


    @Transactional
    public void updateTitle(UpdateQuizTitleRequestDTO request, Long userId) {
        StudyQuiz studyQuiz = studyQuizRepository.findById(request.studyQuizId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyQuiz.getStudy().getUser().getUserId());

        studyQuiz.setTitle(request.quizTitle());
    }

    // 퀴즈 삭제
    @Transactional
    public void deleteStudyQuiz(Long studyQuizId, Long userId) {
        StudyQuiz studyQuiz = studyQuizRepository.findById(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyQuiz.getStudy().getUser().getUserId());
        
        studyQuizRepository.deleteById(studyQuizId);
    }

    // 퀴즈 이력 삭제
    @Transactional
    public void deleteQuizHistory(Long quizHistoryId, Long userId) {
        QuizHistory quizHistory = quizHistoryRepository.findById(quizHistoryId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_HISTORY_NOT_FOUND));
                
        AuthorizationUtil.verifyOwnership(userId, quizHistory.getStudyQuiz().getStudy().getUser().getUserId());

        quizHistoryRepository.deleteById(quizHistoryId);
    }


    // 필기 내용에서 HTML 태그를 뺀 문장만 추출
    private String preprocessNoteContent(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) return "";
        Document doc = Jsoup.parse(htmlContent);
        doc.select("s, strike, del").remove(); // 취소줄 등은 제외
        return doc.text();
    }
}
