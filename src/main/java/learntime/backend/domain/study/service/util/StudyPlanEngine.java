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
     *
     * @param topics   AI가 분배한 일자별 학습 주제 목록
     * @param maxDays  사용자가 설정한 순수 학습 가능 일수 (휴일 제외). 복습 일정이 이 범위를 초과하면 skip.
     */
    public StudyPlanResponseDTO buildFullPlan(List<String> topics, int maxDays) {
        Map<Integer, List<String>> dailyTaskMap = new TreeMap<>();
        int n = topics.size();
        int m = Math.max(n, maxDays - 7);

        for (int i = 0; i < n; i++) {
            int currentDay;
            if (n <= 1) {
                currentDay = 1;
            } else {
                currentDay = (int) Math.round((double) i / (n - 1) * (m - 1)) + 1;
            }
            if (currentDay > maxDays) break;

            String topic = topics.get(i);

            // 빈 topic은 진도/복습 추가 대상에서 제외
            if (topic == null || topic.isBlank()) continue;

            // 1. 당일 신규 학습 추가
            addTask(dailyTaskMap, currentDay, topic);

            // 2. 복습 일정 추가 (1일, 3일, 7일 후) - maxDays를 초과하는 복습은 skip
            if (currentDay + 1 <= maxDays) addReviewTask(dailyTaskMap, currentDay + 1, topic);
            if (currentDay + 3 <= maxDays) addReviewTask(dailyTaskMap, currentDay + 3, topic);
            if (currentDay + 7 <= maxDays) addReviewTask(dailyTaskMap, currentDay + 7, topic);
        }

        // 1일부터 maxDays까지의 모든 일차가 맵에 존재하도록 보장 (사용자가 요청한 기간 일치가 목적)
        for (int day = 1; day <= maxDays; day++) {
            dailyTaskMap.putIfAbsent(day, new ArrayList<>());
        }

        // 3. Map을 DTO 리스트로 변환
        List<StudyPlanResponseDTO.DailyPlan> dailyPlans = dailyTaskMap.entrySet().stream()
                .map(entry -> {
                    String content = String.join(", ", entry.getValue());
                    if (content.isBlank()) {
                        content = "자율 학습 및 휴식";
                    }
                    return new StudyPlanResponseDTO.DailyPlan(
                            entry.getKey(),
                            content
                    );
                })
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
