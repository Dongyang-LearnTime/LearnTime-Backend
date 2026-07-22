package learntime.backend.domain.user.dto.oauth2;

import learntime.backend.domain.user.enums.AuthProvider;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    
    private final Map<String, Object> attributes;
    
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : Map.of();
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        Object sub = attributes.get("sub");
        return sub != null ? String.valueOf(sub) : null;
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? String.valueOf(email) : null;
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        return name != null ? String.valueOf(name) : null;
    }
}
