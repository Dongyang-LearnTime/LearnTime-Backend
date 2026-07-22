package learntime.backend.domain.user.service;

import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.domain.user.dto.oauth2.GoogleOAuth2UserInfo;
import learntime.backend.domain.user.dto.oauth2.OAuth2UserInfo;
import learntime.backend.domain.user.dto.request.SocialLoginRequestDTO;
import learntime.backend.domain.user.dto.request.SocialSignUpRequestDTO;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.domain.user.model.PromptQuotas;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.model.UserTerms;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OAuth2Service {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PromptQuotaRepository promptQuotaRepository;
    private final UserTermsRepository userTermsRepository;
    private final AuthService authService;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.max-quota}")
    private int maxQuota;

    public OAuth2Service(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PromptQuotaRepository promptQuotaRepository,
            UserTermsRepository userTermsRepository,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.promptQuotaRepository = promptQuotaRepository;
        this.userTermsRepository = userTermsRepository;
        this.authService = authService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Transactional
    public Optional<AuthService.TokenPair> socialLogin(SocialLoginRequestDTO request) {
        // 소셜 플랫폼에서 유저 정보 가져오기
        OAuth2UserInfo userInfo = getOAuth2UserInfo(request.provider(), request.token());
        
        // 해당 유저가 DB에 있는지 확인
        Optional<User> optionalUser = userRepository.findBySocialIdAndSocialProvider(userInfo.getProviderId(), userInfo.getProvider());
        
        if (optionalUser.isEmpty()) {
            return Optional.empty(); // 미가입 상태
        }

        User user = optionalUser.get();
        // 계정 잠금 확인
        if (user.isAccountLocked()) {
            throw new AuthException(AuthErrorCode.LOCKED_ACCOUNT);
        }

        // JWT 토큰 발급
        return Optional.of(authService.generateTokenPair(user));
    }

    @Transactional
    public AuthService.TokenPair socialSignUp(SocialSignUpRequestDTO request) {
        // 1. 소셜 플랫폼에서 유저 정보 가져오기 (토큰 검증)
        OAuth2UserInfo userInfo = getOAuth2UserInfo(request.provider(), request.token());
        
        // 2. 이미 가입되어 있는지 확인
        if (userRepository.findBySocialIdAndSocialProvider(userInfo.getProviderId(), userInfo.getProvider()).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_EMAIL_DUPLICATED);
        }

        // 2-2. 이메일 중복 검사
        if (userRepository.existsByEmail(userInfo.getEmail())) {
            throw new AuthException(AuthErrorCode.USER_EMAIL_DUPLICATED);
        }

        // 3. 닉네임 중복 검사
        if (userRepository.existsByName(request.userName())) {
            throw new AuthException(AuthErrorCode.USER_NAME_DUPLICATED);
        }

        // 4. 필수 약관 동의 검사
        boolean allRequiredTermsAgreed = Arrays.stream(Terms.values())
                .filter(Terms::isRequired)
                .allMatch(term -> request.termsAgreements().getOrDefault(term, false));
        if (!allRequiredTermsAgreed) {
            throw new AuthException(AuthErrorCode.TERMS_NOT_AGREED);
        }

        // 5. User 엔티티 생성
        User user = User.builder()
                .email(userInfo.getEmail()) 
                .name(request.userName())
                .socialId(userInfo.getProviderId())
                .socialProvider(userInfo.getProvider())
                .role(Role.ROLE_USER)
                .password(null) 
                .build();
        User savedUser = userRepository.save(user);

        // 6. 약관 저장
        List<UserTerms> userTermsList = request.termsAgreements().entrySet().stream()
                .map(entry -> UserTerms.builder()
                        .user(savedUser)
                        .terms(entry.getKey())
                        .agreed(entry.getValue())
                        .agreedAt(LocalDateTime.now())
                        .build())
                .toList();
        userTermsRepository.saveAll(userTermsList);

        // 7. 할당량 및 프로필 생성
        promptQuotaRepository.save(new PromptQuotas(savedUser, maxQuota));
        profileRepository.save(Profile.builder().user(savedUser).profileVisibility(ProfileVisibility.PUBLIC).build());

        // 8. 로그인 처리 (토큰 반환)
        return authService.generateTokenPair(savedUser);
    }

    private OAuth2UserInfo getOAuth2UserInfo(AuthProvider provider, String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        if (provider == AuthProvider.GOOGLE) {
            Map<String, Object> attributes;
            try {
                // ID Token(JWT 형태) 인지 Access Token 인지 분기 검증
                if (isIdToken(token)) {
                    String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            tokenInfoUrl,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );
                    attributes = response.getBody();
                } else {
                    String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);
                    HttpEntity<String> entity = new HttpEntity<>("", headers);

                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            userInfoUrl,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<>() {}
                    );
                    attributes = response.getBody();
                }

                GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

                if (userInfo.getProviderId() == null || userInfo.getProviderId().isBlank() ||
                    userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
                    log.error("Google OAuth2 user info validation failed: missing sub or email");
                    throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
                }

                return userInfo;
            } catch (RestClientException e) {
                log.error("Google API Error during token verification: {}", e.getMessage());
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }
        } else if (provider == AuthProvider.NAVER) {
            throw new UnsupportedOperationException("네이버 로그인은 아직 지원하지 않습니다.");
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
    }

    private boolean isIdToken(String token) {
        return token.startsWith("eyJ") && token.contains(".");
    }

    public void revokeSocialToken(AuthProvider provider, String token) {
        if (token == null || token.isBlank()) {
            log.info("Social token is empty. Skipping revoke.");
            return;
        }

        if (provider == AuthProvider.GOOGLE) {
            String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + token;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                HttpEntity<String> entity = new HttpEntity<>("", headers);

                restTemplate.postForEntity(revokeUrl, entity, String.class);
            } catch (RestClientException e) {
                log.warn("Failed to revoke Google token. It might already be invalid: {}", e.getMessage());
            }
        }
    }
}
