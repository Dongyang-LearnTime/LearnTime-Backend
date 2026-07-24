package learntime.backend.domain.user.dto.oauth2;

import learntime.backend.domain.user.enums.AuthProvider;

public interface OAuth2UserInfo {
    AuthProvider getProvider();
    String getProviderId();
    String getEmail();
    String getName();
}
