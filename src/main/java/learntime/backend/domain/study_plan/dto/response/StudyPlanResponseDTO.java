package learntime.backend.domain.study_plan.dto.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = StudyPlanResponseDTO.TupleDeserializer.class)
@Schema(description = "Gemini 응답 Tuple 데이터를 JSON으로 바꿀때 쓰는 DTO")
public record StudyPlanResponseDTO(
        List<DailyPlan> dailyPlans
) {
    public record DailyPlan(
            Integer day,
            String tasks
    ) {}

    public static class TupleDeserializer extends JsonDeserializer<StudyPlanResponseDTO> {
        @Override
        public StudyPlanResponseDTO deserialize(JsonParser p, DeserializationContext text) throws IOException {
            JsonNode root = p.getCodec().readTree(p);
            List<DailyPlan> plans = new ArrayList<>();

            for (JsonNode dayNode : root) {
                // 배열 형태(Tuple)의 JSON 노드를 인덱스로 파싱: [ [1, "할일"], [2, "할일"] ]
                Integer day = dayNode.get(0).asInt();
                String tasks = dayNode.get(1).asText();

                plans.add(new DailyPlan(day, tasks));
            }

            return new StudyPlanResponseDTO(List.copyOf(plans));
        }
    }
}
