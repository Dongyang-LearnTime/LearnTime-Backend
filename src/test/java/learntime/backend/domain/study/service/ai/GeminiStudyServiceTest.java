package learntime.backend.domain.study.service.ai;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.dto.response.TocListResponseDTO;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.utils.GeminiPromptParser;
import learntime.backend.global.utils.PromptQuotaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiStudyServiceTest {

    @Mock private GeminiClient geminiClient;
    @Mock private GeminiPromptParser promptParser;
    @Mock private PromptQuotaUtil promptQuotaUtil;
    @Mock private learntime.backend.domain.study.service.util.StudyPlanEngine studyPlanEngine;

    @InjectMocks private GeminiStudyService geminiStudyService;

    @BeforeEach
    void setUp() throws Exception {
        var resource = new ByteArrayResource("테스트 프롬프트: %d %s".getBytes());
        ReflectionTestUtils.setField(geminiStudyService, "promptResource", resource);
        ReflectionTestUtils.setField(geminiStudyService, "replanPromptResource", resource);
        geminiStudyService.init();
    }

    @Test
    void generateSmartStudyPlan_ShouldReturnResponse() throws Exception {
        GeminiStudyRequestDTO request = new GeminiStudyRequestDTO(
                "테스트 도서",
                "테스트 진도",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(31),
                List.of(new TocListResponseDTO("1", "기초", 10), new TocListResponseDTO("2", "심화", 20)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(geminiClient.sendRequest(any(), any())).thenReturn("{}");
        when(promptParser.parseListResponse(any())).thenReturn(List.of("1장", "2장"));
        when(studyPlanEngine.buildFullPlan(any())).thenReturn(new StudyPlanResponseDTO(Collections.emptyList()));

        StudyPlanResponseDTO result = geminiStudyService.generateSmartStudyPlan(request, 1L);

        assertNotNull(result);
    }
}
