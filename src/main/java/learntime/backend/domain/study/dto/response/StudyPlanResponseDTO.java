package learntime.backend.domain.study.dto.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = StudyPlanResponseDTO.TupleDeserializer.class)
public class StudyPlanResponseDTO {

    private List<DailyPlan> dailyPlans;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DailyPlan {
        private int day;
        private String tasks;
    }

    public static class TupleDeserializer extends JsonDeserializer<StudyPlanResponseDTO> {
        @Override
        public StudyPlanResponseDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode root = p.getCodec().readTree(p);
            List<DailyPlan> plans = new ArrayList<>();

            for (JsonNode dayNode : root) {
                int day = dayNode.get(0).asInt();
                String tasks = dayNode.get(1).asText();

                plans.add(new DailyPlan(day, tasks));
            }

            return new StudyPlanResponseDTO(plans);
        }
    }
}