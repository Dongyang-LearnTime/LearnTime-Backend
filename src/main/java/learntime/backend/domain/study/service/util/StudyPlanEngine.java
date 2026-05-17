package learntime.backend.domain.study.service.util;

import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class StudyPlanEngine {

    /**
     * AI가 분배한 목차 리스트를 받아 에빙하우스 복습 주기를 적용한 전체 학습 계획을 생성한다.
     * 복습 주기: 1일 후, 3일 후, 7일 후
     */
    public StudyPlanResponseDTO buildFullPlan(List<String> topics) {
        Map<Integer, List<String>> dailyTaskMap = new TreeMap<>();

        for (int i = 0; i < topics.size(); i++) {
            int currentDay = i + 1;
            String topic = topics.get(i);

            // 1. 당일 신규 학습 추가
            addTask(dailyTaskMap, currentDay, topic);

            // 2. 복습 일정 추가 (1일, 3일, 7일 후)
            addReviewTask(dailyTaskMap, currentDay + 1, topic);
            addReviewTask(dailyTaskMap, currentDay + 3, topic);
            addReviewTask(dailyTaskMap, currentDay + 7, topic);
        }

        // 3. Map을 DTO 리스트로 변환
        List<StudyPlanResponseDTO.DailyPlan> dailyPlans = dailyTaskMap.entrySet().stream()
                .map(entry -> new StudyPlanResponseDTO.DailyPlan(
                        entry.getKey(),
                        String.join(", ", entry.getValue())
                ))
                .collect(Collectors.toList());

        return new StudyPlanResponseDTO(dailyPlans);
    }

    private void addTask(Map<Integer, List<String>> map, int day, String task) {
        map.computeIfAbsent(day, k -> new ArrayList<>()).add(task);
    }

    private void addReviewTask(Map<Integer, List<String>> map, int day, String topic) {
        String reviewTask = "[복습] " + topic;
        map.computeIfAbsent(day, k -> new ArrayList<>()).add(reviewTask);
    }
}
