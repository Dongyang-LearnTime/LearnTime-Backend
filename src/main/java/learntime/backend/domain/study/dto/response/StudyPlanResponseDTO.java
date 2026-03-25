package learntime.backend.domain.study.dto.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = StudyPlanResponseDTO.TupleDeserializer.class)
public record StudyPlanResponseDTO(
        List<DailyPlan> dailyPlans
) {
    public record DailyPlan(
            int day,
            String tasks
    ) {}

    public static class TupleDeserializer extends JsonDeserializer<StudyPlanResponseDTO> {
        @Override
        public StudyPlanResponseDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode root = p.getCodec().readTree(p);
            List<DailyPlan> plans = new ArrayList<>();

            for (JsonNode dayNode : root) {
                // 배열 형태(Tuple)의 JSON 노드를 인덱스로 파싱: [ [1, "할일"], [2, "할일"] ]
                int day = dayNode.get(0).asInt();
                String tasks = dayNode.get(1).asText();

                plans.add(new DailyPlan(day, tasks));
            }

            return new StudyPlanResponseDTO(List.copyOf(plans));
        }
    }
}