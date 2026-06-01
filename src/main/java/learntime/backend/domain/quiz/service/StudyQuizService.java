package learntime.backend.domain.quiz.service;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.quiz.dto.request.QuizSolveRequestDTO;
import learntime.backend.domain.quiz.dto.response.QuizHistoryInfoResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizInfoResponseDTO;
import learntime.backend.domain.quiz.dto.response.QuizQuestionResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.quiz.model.StudyQuiz;
import learntime.backend.domain.quiz.model.QuizQuestion;
import learntime.backend.domain.quiz.converter.StudyQuizConverter;
import learntime.backend.domain.quiz.repository.QuizQuestionRepository;
import learntime.backend.domain.quiz.repository.StudyQuizRepository;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import learntime.backend.domain.badge.event.QuizCompletedEvent;
import learntime.backend.domain.quiz.model.QuizAnswer;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.quiz.repository.QuizHistoryRepository;

@Service
@RequiredArgsConstructor
public class StudyQuizService {

    private final StudyQuizRepository studyQuizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int CORRECT_ANSWER_BONUS = 5; // 정답 하나 당 추가 포인트

    @Transactional(readOnly = true)
    public PageResponse<StudyQuizInfoResponseDTO> getStudyQuizList(Long studyMemberId, Pageable pageable) {
        Page<StudyQuiz> quizzes = studyQuizRepository.findAllByStudyMember_StudyMemberId(studyMemberId, pageable);
        Page<StudyQuizInfoResponseDTO> dtoList = quizzes.map(
                StudyQuizConverter::toStudyQuizInfoResponseDTO
        );
        return PageResponse.of(dtoList);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizHistoryInfoResponseDTO> getQuizHistoryList(Long studyQuizId, Pageable pageable) {
        Page<QuizHistory> histories = quizHistoryRepository.findAllWithAnswersByStudyQuizId(studyQuizId, pageable);
        Page<QuizHistoryInfoResponseDTO> dtoList = histories.map(
                StudyQuizConverter::toQuizHistoryInfoResponseDTO
        );
        return PageResponse.of(dtoList);
    }

    @Transactional(readOnly = true)
    public StudyQuizResultResponseDTO getQuizResult(Long quizHistoryId) {
        QuizHistory quizHistory = quizHistoryRepository.findById(quizHistoryId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.QUIZ_HISTORY_NOT_FOUND));

        return StudyQuizConverter.
                toStudyQuizResultResponseDTO(quizHistory, null);
    }

    @Transactional
    public Long saveStudyQuiz(StudyMember studyMember, List<QuizQuestionResponseDTO> questionDos) {
        String quizTitle = "퀴즈 " + UUID.randomUUID().toString().substring(0, 8);

        StudyQuiz quiz = StudyQuizConverter.toStudyQuizEntity(studyMember, quizTitle);

        List<QuizQuestion> questions = questionDos.stream()
                .map(dto -> StudyQuizConverter.toQuizQuestionEntity(quiz, dto))
                .toList();

        quiz.getQuestions().addAll(questions);

        StudyQuiz savedQuiz = studyQuizRepository.save(quiz);
        return savedQuiz.getStudyQuizId();
    }

    @Transactional
    @CacheEvict(value = "studyTotalIndicator", allEntries = true)
    public Long solveQuiz(List<QuizSolveRequestDTO> requests, Long userId) {
        List<Long> questionIds = requests.stream()
                .map(QuizSolveRequestDTO::quizQuestionId)
                .toList();

        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);
        
        if (questions.size() != requests.size() || questions.isEmpty()) {
            throw new StudyException(StudyErrorCode.QUIZ_QUESTION_NOT_FOUND);
        }

        StudyQuiz studyQuiz = questions.getFirst().getStudyQuiz(); // 공부 퀴즈 정보 가져옴
        
        // 본인만 본인의 퀴즈를 풀 수 있음
        if (!studyQuiz.getStudyMember().getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }

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

        int quizSolvePoint = 0;
        // 푼 횟수가 1인(처음 푸는) 경우에만 포인트 지급 (completeQuiz() 호출 전이므로 0일때 처음 푼 것임)
        if (isFirstTime) {
            quizSolvePoint = PointPolicy.STUDY_QUIZ_COMPLETED.getAmount() + (CORRECT_ANSWER_BONUS * correctCount);
        }

        // 퀴즈 상태 및 푼 횟수 증가
        studyQuiz.completeQuiz();
        int attemptNumber = studyQuiz.getCompletedCount();

        // 퀴즈 풀이 이력 생성 및 저장
        QuizHistory quizHistory = StudyQuizConverter.toQuizHistoryEntity(studyQuiz, attemptNumber, correctCount, quizSolvePoint);

        List<QuizAnswer> quizAnswers = requests.stream().map(request -> {
            QuizQuestion question = questionMap.get(request.quizQuestionId());
            boolean isCorrect = question.getCorrectAnswer().equals(request.userAnswer());
            return StudyQuizConverter.toQuizAnswerEntity(quizHistory, question, request.userAnswer(), isCorrect);
        }).toList();
        
        quizHistory.getAnswers().addAll(quizAnswers);
        quizHistoryRepository.save(quizHistory);

        // 푼 횟수가 1인(처음 푸는) 경우에만 포인트 지급
        if (isFirstTime) {
            String eventDescription = PointPolicy.STUDY_QUIZ_COMPLETED.getDescription()
                    + " (문제 " + questions.size() + "개 중 정답: " + correctCount + "개)";

            eventPublisher.publishEvent(new PointEventDTO(
                    userId, // 사용자 ID
                    quizSolvePoint, // 부여 할 포인트
                    PointType.EARN, // 포인트 부여 유형 (증가, 감소, 취소)
                    eventDescription // 포인트 이벤트 설명
            ));
        }
        
        boolean isPerfect = (correctCount == questions.size() && questions.size() > 0);
        eventPublisher.publishEvent(new QuizCompletedEvent(userId, isPerfect, LocalDateTime.now()));

        return quizHistory.getQuizHistoryId();
    }
}
