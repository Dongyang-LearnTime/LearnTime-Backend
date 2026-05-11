package learntime.backend.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeminiModel {
    GEMINI_3_0("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"),
    GEMINI_3_1("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent");

    private final String endpoint; // 실제 API URL
}
