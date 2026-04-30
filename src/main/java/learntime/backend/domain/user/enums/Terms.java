package learntime.backend.domain.user.enums;

import lombok.Getter;

@Getter
public enum Terms {
    SERVICE_USE("서비스 이용약관", true),
    PRIVACY_POLICY("개인정보 수집 및 이용 동의", true),
    BODY_DATA_COLLECT("신체 데이터 수집 및 이용 동의", false); // 선택 동의 가정

    private final String termName;
    private final boolean required;

    Terms(String termName, boolean required) {
        this.termName = termName;
        this.required = required;
    }

}