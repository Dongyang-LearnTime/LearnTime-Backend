package learntime.backend.domain.quiz.converter;

import learntime.backend.domain.quiz.dto.response.QuizQuestionResponseDTO;
import learntime.backend.domain.quiz.dto.response.QuizHistoryListResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizListResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.quiz.model.QuizAnswer;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.quiz.model.QuizQuestion;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.quiz.model.StudyQuiz;

import java.util.List;

public class StudyQuizConverter {

    private StudyQuizConverter() {}

    public static StudyQuizResponseDTO toResponseDTO(StudyQuiz studyQuiz, List<QuizQuestion> questions) {
        // JDK 16+ Stream.toList() 활용 (불변 리스트 반환)
        List<StudyQuizResponseDTO.QuizQuestionInfoDTO> questionDTOs = questions.stream()
                .map(StudyQuizConverter::toQuestionInfoDTO)
                .toList();

        return new StudyQuizResponseDTO(
                studyQuiz.getQuizTitle(),
                studyQuiz.getQuizStatus(),
                questionDTOs
        );
    }

    private static StudyQuizResponseDTO.QuizQuestionInfoDTO toQuestionInfoDTO(QuizQuestion question) {
        return StudyQuizResponseDTO.QuizQuestionInfoDTO.builder()
                .quizQuestionId(question.getQuizQuestionId())
                .questionContent(question.getQuestionContent())
                .quizType(question.getQuizType())
                .build();
    }

    public static StudyQuiz toStudyQuizEntity(Study study, String quizTitle) {
        return StudyQuiz.builder()
                .study(study)
                .quizTitle(quizTitle)
                .build();
    }

    public static QuizQuestion toQuizQuestionEntity(StudyQuiz quiz, QuizQuestionResponseDTO dto) {
        return QuizQuestion.builder()
                .studyQuiz(quiz)
                .questionContent(dto.questionContent())
                .correctAnswer(dto.correctAnswer())
                .quizType(dto.quizType())
                .build();
    }

    public static QuizHistory toQuizHistoryEntity(StudyQuiz studyQuiz, int attemptNumber, int correctCount) {
        return QuizHistory.builder()
                .studyQuiz(studyQuiz)
                .attemptNumber(attemptNumber)
                .correctCount(correctCount)
                .build();
    }

    public static QuizAnswer toQuizAnswerEntity(QuizHistory quizHistory, QuizQuestion quizQuestion, String userAnswer, boolean isCorrect) {
        return QuizAnswer.builder()
                .quizHistory(quizHistory)
                .quizQuestion(quizQuestion)
                .userAnswer(userAnswer)
                .isCorrect(isCorrect)
                .build();
    }

    public static StudyQuizResultResponseDTO.QuizDetailResponseDTO toQuizDetailResponseDTO(QuizAnswer quizAnswer) {
        return StudyQuizResultResponseDTO.QuizDetailResponseDTO.builder()
                .quizQuestionId(quizAnswer.getQuizQuestion().getQuizQuestionId())
                .questionContent(quizAnswer.getQuizQuestion().getQuestionContent())
                .userAnswer(quizAnswer.getUserAnswer())
                .correctAnswer(quizAnswer.getQuizQuestion().getCorrectAnswer())
                .isCorrect(quizAnswer.getIsCorrect())
                .quizType(quizAnswer.getQuizQuestion().getQuizType())
                .build();
    }

    public static StudyQuizResultResponseDTO toStudyQuizResultResponseDTO(QuizHistory quizHistory, Integer earnedPoints) {
        List<StudyQuizResultResponseDTO.QuizDetailResponseDTO> quizResults = quizHistory.getAnswers().stream()
                .map(StudyQuizConverter::toQuizDetailResponseDTO)
                .toList();

        return StudyQuizResultResponseDTO.builder()
                .totalQuestionCount(quizHistory.getAnswers().size())
                .correctQuestionCount(quizHistory.getCorrectCount())
                .earnedPoints(earnedPoints)
                .quizResults(quizResults)
                .build();
    }

    public static StudyQuizListResponseDTO toStudyQuizListResponseDTO(List<StudyQuiz> studyQuizzes) {
        List<StudyQuizListResponseDTO.StudyQuizInfoDTO> quizDTOs = studyQuizzes.stream()
                .map(quiz -> StudyQuizListResponseDTO.StudyQuizInfoDTO.builder()
                        .studyQuizId(quiz.getStudyQuizId())
                        .quizTitle(quiz.getQuizTitle())
                        .quizStatus(quiz.getQuizStatus())
                        .completedCount(quiz.getCompletedCount())
                        .createdAt(quiz.getCreatedAt())
                        .build())
                .toList();

        return StudyQuizListResponseDTO.builder()
                .quizzes(quizDTOs)
                .build();
    }

    public static QuizHistoryListResponseDTO toQuizHistoryListResponseDTO(List<QuizHistory> quizHistories) {
        List<QuizHistoryListResponseDTO.QuizHistoryInfoDTO> historyDTOs = quizHistories.stream()
                .map(history -> QuizHistoryListResponseDTO.QuizHistoryInfoDTO.builder()
                        .quizHistoryId(history.getQuizHistoryId())
                        .attemptNumber(history.getAttemptNumber())
                        .correctCount(history.getCorrectCount())
                        .totalQuestionCount(history.getAnswers().size())
                        .submittedAt(history.getSubmittedAt())
                        .build())
                .toList();

        return QuizHistoryListResponseDTO.builder()
                .histories(historyDTOs)
                .build();
    }
}
