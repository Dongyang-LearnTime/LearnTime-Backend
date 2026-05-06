package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.dto.request.QuizSolveRequestDTO;
import learntime.backend.domain.study.dto.request.UpdateQuizTitleRequestDTO;
import learntime.backend.domain.study.dto.response.QuizHistoryListResponseDTO;
import learntime.backend.domain.study.dto.response.StudyQuizListResponseDTO;
import learntime.backend.domain.study.dto.response.QuizQuestionResponseDTO;
import learntime.backend.domain.study.dto.response.StudyQuizResponseDTO;
import learntime.backend.domain.study.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyQuiz;
import learntime.backend.domain.study.model.QuizQuestion;
import learntime.backend.domain.study.converter.StudyQuizConverter;
import learntime.backend.domain.study.repository.QuizQuestionRepository;
import learntime.backend.domain.study.repository.StudyQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import learntime.backend.domain.study.model.QuizAnswer;
import learntime.backend.domain.study.model.QuizHistory;
import learntime.backend.domain.study.repository.QuizHistoryRepository;

@Service
@RequiredArgsConstructor
public class StudyQuizService {

    private final StudyQuizRepository studyQuizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int CORRECT_ANSWER_BONUS = 5; // 정답 하나 당 추가 포인트

    @Transactional(readOnly = true)
    public StudyQuizListResponseDTO getStudyQuizList(Long studyId) {
        List<StudyQuiz> quizzes = studyQuizRepository.findAllByStudy_StudyIdOrderByCreatedAtDesc(studyId);
        return StudyQuizConverter.toStudyQuizListResponseDTO(quizzes);
    }

    @Transactional(readOnly = true)
    public QuizHistoryListResponseDTO getQuizHistoryList(Long studyQuizId) {
        List<QuizHistory> histories = quizHistoryRepository.findAllWithAnswersByStudyQuizId(studyQuizId);
        return StudyQuizConverter.toQuizHistoryListResponseDTO(histories);
    }

    @Transactional
    public Long saveStudyQuiz(Study study, List<QuizQuestionResponseDTO> questionDos) {
        String quizTitle = study.getStudyTitle() + " 퀴즈 " + UUID.randomUUID().toString().substring(0, 8);

        StudyQuiz quiz = StudyQuizConverter.toStudyQuizEntity(study, quizTitle);

        List<QuizQuestion> questions = questionDos.stream()
                .map(dto -> StudyQuizConverter.toQuizQuestionEntity(quiz, dto))
                .toList();

        quiz.getQuestions().addAll(questions);

        StudyQuiz savedQuiz = studyQuizRepository.save(quiz);
        return savedQuiz.getStudyQuizId();
    }

    @Transactional
    public StudyQuizResultResponseDTO solveQuiz(List<QuizSolveRequestDTO> requests, Long userId) {
        List<Long> questionIds = requests.stream()
                .map(QuizSolveRequestDTO::quizQuestionId)
                .toList();

        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);
        
        if (questions.size() != requests.size() || questions.isEmpty()) {
            throw new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND);
        }

        StudyQuiz studyQuiz = questions.getFirst().getStudyQuiz(); // 공부 퀴즈 정보 가져옴
        boolean isFirstTime = studyQuiz.getCompletedCount() == 0; // 처음 푸는지 여부 확인

        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getQuizQuestionId, q -> q));

        // 정답 개수 계산
        int correctCount = 0;
        for (QuizSolveRequestDTO request : requests) {
            QuizQuestion question = questionMap.get(request.quizQuestionId());
            if (question.getCorrectAnswer().equals(request.userAnswer())) {
                correctCount++;
            }
        }

        // 퀴즈 상태 및 푼 횟수 증가
        studyQuiz.completeQuiz();
        int attemptNumber = studyQuiz.getCompletedCount();

        // 퀴즈 풀이 이력 생성 및 저장
        QuizHistory quizHistory = StudyQuizConverter.toQuizHistoryEntity(studyQuiz, attemptNumber, correctCount);

        List<QuizAnswer> quizAnswers = requests.stream().map(request -> {
            QuizQuestion question = questionMap.get(request.quizQuestionId());
            boolean isCorrect = question.getCorrectAnswer().equals(request.userAnswer());
            return StudyQuizConverter.toQuizAnswerEntity(quizHistory, question, request.userAnswer(), isCorrect);
        }).toList();
        
        quizHistory.getAnswers().addAll(quizAnswers);
        quizHistoryRepository.save(quizHistory);

        int quizSolvePoint = 0;
        // 푼 횟수가 1인(처음 푸는) 경우에만 포인트 지급 (completeQuiz() 호출 후이므로 1일때 처음 푼 것임)
        if (isFirstTime) {
            quizSolvePoint = PointPolicy.STUDY_QUIZ_COMPLETED.getAmount() + (CORRECT_ANSWER_BONUS * correctCount);
            String eventDescription = PointPolicy.STUDY_QUIZ_COMPLETED.getDescription()
                    + " (문제 " + questions.size() + "개 중 정답: " + correctCount + "개)";

            eventPublisher.publishEvent(new PointEventDTO(
                    userId,
                    quizSolvePoint,
                    PointType.EARN,
                    eventDescription
            ));
        }

        return StudyQuizConverter.
                toStudyQuizResultResponseDTO(quizHistory, quizSolvePoint);
    }

    @Transactional(readOnly = true)
    public StudyQuizResultResponseDTO getQuizResult(Long studyQuizId) {
        QuizHistory quizHistory = quizHistoryRepository
                .findFirstByStudyQuiz_StudyQuizIdOrderByAttemptNumberDesc(studyQuizId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_HISTORY_NOT_FOUND));

        return StudyQuizConverter.
                toStudyQuizResultResponseDTO(quizHistory, null);
    }

}
