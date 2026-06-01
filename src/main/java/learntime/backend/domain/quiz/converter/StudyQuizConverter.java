package learntime.backend.domain.quiz.converter;

import learntime.backend.domain.quiz.dto.response.QuizQuestionResponseDTO;
import learntime.backend.domain.quiz.dto.response.QuizHistoryInfoResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizInfoResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResponseDTO;
import learntime.backend.domain.quiz.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.quiz.model.QuizAnswer;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.quiz.model.QuizQuestion;
import learntime.backend.domain.quiz.model.StudyQuiz;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.util.List;

public class StudyQuizConverter {

    private StudyQuizConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyQuizResponseDTO toResponseDTO(StudyQuiz studyQuiz, List<QuizQuestion> questions) {
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

    public static StudyQuiz toStudyQuizEntity(StudyMember studyMember, String quizTitle) {
        return StudyQuiz.builder()
                .studyMember(studyMember)
                .quizTitle(quizTitle)
                .build();
    }

    public static QuizQuestion toQuizQuestionEntity(StudyQuiz quiz, QuizQuestionResponseDTO dto) {
        String sanitizedContent = dto.questionContent();
        if (sanitizedContent != null) {
            // 연속된 줄바꿈(\n\n+)을 단일 줄바꿈(\n)으로 변환
            sanitizedContent = sanitizedContent.replaceAll("\\n+", "\n").trim();
        }

        return QuizQuestion.builder()
                .studyQuiz(quiz)
                .questionContent(sanitizedContent)
                .correctAnswer(dto.correctAnswer())
                .quizType(dto.quizType())
                .build();
    }

    public static QuizHistory toQuizHistoryEntity(StudyQuiz studyQuiz, int attemptNumber, int correctCount, Integer earnedPoints) {
        return QuizHistory.builder()
                .studyQuiz(studyQuiz)
                .attemptNumber(attemptNumber)
                .correctCount(correctCount)
                .earnedPoints(earnedPoints)
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
                .earnedPoints(earnedPoints != null ? earnedPoints : quizHistory.getEarnedPoints())
                .quizResults(quizResults)
                .build();
    }

    public static StudyQuizInfoResponseDTO toStudyQuizInfoResponseDTO(StudyQuiz studyQuiz) {
        return StudyQuizInfoResponseDTO.builder()
                .studyQuizId(studyQuiz.getStudyQuizId())
                .quizTitle(studyQuiz.getQuizTitle())
                .quizStatus(studyQuiz.getQuizStatus())
                .completedCount(studyQuiz.getCompletedCount())
                .createdAt(studyQuiz.getCreatedAt())
                .build();
    }

    public static QuizHistoryInfoResponseDTO toQuizHistoryInfoResponseDTO(QuizHistory quizHistories) {
        return QuizHistoryInfoResponseDTO.builder()
                .quizHistoryId(quizHistories.getQuizHistoryId())
                .attemptNumber(quizHistories.getAttemptNumber())
                .correctCount(quizHistories.getCorrectCount())
                .totalQuestionCount(quizHistories.getAnswers().size())
                .earnedPoints(quizHistories.getEarnedPoints())
                .submittedAt(quizHistories.getSubmittedAt())
                .build();
    }
}
