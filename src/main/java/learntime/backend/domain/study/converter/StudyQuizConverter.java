package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.response.QuizQuestionResponseDTO;
import learntime.backend.domain.study.dto.response.StudyQuizResponseDTO;
import learntime.backend.domain.study.dto.response.StudyQuizResultResponseDTO;
import learntime.backend.domain.study.model.QuizAnswer;
import learntime.backend.domain.study.model.QuizHistory;
import learntime.backend.domain.study.model.QuizQuestion;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyQuiz;

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
}
