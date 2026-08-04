package learntime.backend.domain.user.service;

import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.domain.user.dto.oauth2.GoogleOAuth2UserInfo;
import learntime.backend.domain.user.dto.oauth2.KakaoOAuth2UserInfo;
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

/**
 * 소셜 로그인(구글, 카카오 등) 및 소셜 회원가입, 연동 해지를 전담하는 서비스 클래스.
 */
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

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String kakaoUserInfoUri;

    @Value("${spring.security.oauth2.client.provider.kakao.logout-uri:https://kapi.kakao.com/v1/user/logout}")
    private String kakaoLogoutUri;

    @Value("${spring.security.oauth2.client.provider.kakao.unlink-uri:https://kapi.kakao.com/v1/user/unlink}")
    private String kakaoUnlinkUri;

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

    /**
     * 소셜 로그인 인증 처리.
     * 1. 전달받은 소셜 토큰으로 사용자 정보를 조회하고 검증합니다.
     * 2. 가입된 회원이라면 계정 잠금 여부를 확인 후 토큰 쌍(Access Token & Refresh Token)을 발급합니다.
     * 3. 미가입 회원이면 Optional.empty()를 반환합니다.
     */
    @Transactional
    public Optional<AuthService.TokenPair> socialLogin(SocialLoginRequestDTO request) {
        OAuth2UserInfo userInfo = getOAuth2UserInfo(request.provider(), request.token());

        Optional<User> optionalUser = userRepository.findBySocialIdAndSocialProvider(
                userInfo.getProviderId(), userInfo.getProvider()
        );

        if (optionalUser.isEmpty()) {
            log.info("미가입 소셜 유저 로그인 시도: provider={}, socialId={}", userInfo.getProvider(), userInfo.getProviderId());
            return Optional.empty();
        }

        User user = optionalUser.get();

        if (user.isAccountLocked()) {
            throw new AuthException(AuthErrorCode.LOCKED_ACCOUNT);
        }

        return Optional.of(authService.generateTokenPair(user));
    }

    /**
     * 신규 소셜 회원가입 처리.
     * 1. 소셜 토큰 검증 및 유저 프로필 수집.
     * 2. 기존 가입 여부, 이메일 중복 및 닉네임 중복 검사.
     * 3. 필수 서비스 약관 동의 검증.
     * 4. 신규 유저 생성, 약관 저장, 프롬프트 할당량 및 기본 프로필 생성.
     * 5. 로그인 처리 후 토큰 쌍 반환.
     */
    @Transactional
    public AuthService.TokenPair socialSignUp(SocialSignUpRequestDTO request) {
        // Step 1: 소셜 토큰 검증 및 유저 정보 조회
        OAuth2UserInfo userInfo = getOAuth2UserInfo(request.provider(), request.token());

        // Step 2: 동일 소셜계정 및 이메일 중복 검사
        if (userRepository.findBySocialIdAndSocialProvider(userInfo.getProviderId(), userInfo.getProvider()).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_EMAIL_DUPLICATED);
        }
        if (userInfo.getEmail() != null && userRepository.existsByEmail(userInfo.getEmail())) {
            throw new AuthException(AuthErrorCode.USER_EMAIL_DUPLICATED);
        }

        // Step 3: 닉네임 중복 검사
        if (userRepository.existsByName(request.userName())) {
            throw new AuthException(AuthErrorCode.USER_NAME_DUPLICATED);
        }

        // Step 4: 필수 약관 동의 확인
        boolean allRequiredTermsAgreed = Arrays.stream(Terms.values())
                .filter(Terms::isRequired)
                .allMatch(term -> request.termsAgreements().getOrDefault(term, false));
        if (!allRequiredTermsAgreed) {
            throw new AuthException(AuthErrorCode.TERMS_NOT_AGREED);
        }

        // Step 5: 유저 엔티티 생성 및 저장
        User user = User.builder()
                .email(userInfo.getEmail())
                .name(request.userName())
                .socialId(userInfo.getProviderId())
                .socialProvider(userInfo.getProvider())
                .role(Role.ROLE_USER)
                .password(null)
                .build();
        User savedUser = userRepository.save(user);

        // Step 6: 동의 내역 저장
        List<UserTerms> userTermsList = request.termsAgreements().entrySet().stream()
                .map(entry -> UserTerms.builder()
                        .user(savedUser)
                        .terms(entry.getKey())
                        .agreed(entry.getValue())
                        .agreedAt(LocalDateTime.now())
                        .build())
                .toList();
        userTermsRepository.saveAll(userTermsList);

        // Step 7: 초기 할당량 및 기본 프로필 설정
        promptQuotaRepository.save(new PromptQuotas(savedUser, maxQuota));
        profileRepository.save(Profile.builder().user(savedUser).profileVisibility(ProfileVisibility.PUBLIC).build());

        log.info("신규 소셜 회원가입 완료: userId={}, name={}, provider={}", savedUser.getUserId(), savedUser.getName(), savedUser.getSocialProvider());

        // Step 8: 로그인 토큰 발급
        return authService.generateTokenPair(savedUser);
    }

    /**
     * 각 소셜 제공자(GOOGLE, KAKAO)별 토큰 검증 및 사용자 정보 반환.
     */
    private OAuth2UserInfo getOAuth2UserInfo(AuthProvider provider, String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        if (provider == AuthProvider.GOOGLE) {
            return getGoogleUserInfo(token);
        } else if (provider == AuthProvider.KAKAO) {
            return getKakaoUserInfo(token);
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
    }

    /**
     * 구글 OAuth2 사용자 정보 조회 (ID Token 또는 Access Token 분기 처리)
     */
    private OAuth2UserInfo getGoogleUserInfo(String token) {
        Map<String, Object> attributes;
        try {
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
                attributes = fetchAttributesWithBearerToken(userInfoUrl, token);
            }

            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

            if (userInfo.getProviderId() == null || userInfo.getProviderId().isBlank() ||
                userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
                log.error("Google OAuth2 검증 실패: sub 또는 email 누락");
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }

            return userInfo;
        } catch (RestClientException e) {
            log.error("Google API 검증 연동 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    /**
     * 카카오 OAuth2 사용자 정보 조회 (Rest API User Info)
     */
    private OAuth2UserInfo getKakaoUserInfo(String token) {
        try {
            Map<String, Object> attributes = fetchAttributesWithBearerToken(kakaoUserInfoUri, token);
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            if (userInfo.getProviderId() == null || userInfo.getProviderId().isBlank()) {
                log.error("Kakao OAuth2 검증 실패: id 누락");
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }

            return userInfo;
        } catch (RestClientException e) {
            log.error("Kakao API 검증 연동 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    /**
     * 소셜 토큰 만료/로그아웃 처리.
     */
    public void revokeSocialToken(AuthProvider provider, String token) {
        if (token == null || token.isBlank()) {
            log.info("소셜 토큰이 없어 무시합니다.");
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
                log.warn("Google 토큰 철회 실패: {}", e.getMessage());
            }
        } else if (provider == AuthProvider.KAKAO) {
            try {
                postWithBearerToken(kakaoLogoutUri, token);
            } catch (RestClientException e) {
                log.warn("Kakao 토큰 로그아웃 실패: {}", e.getMessage());
            }
        }
    }

    /**
     * 소셜 서비스 연결 해지 (Unlink).
     * 카카오 애플리케이션 연결을 완전히 해지(연동 해지)합니다.
     */
    public void unlinkSocialAccount(AuthProvider provider, String token) {
        if (token == null || token.isBlank()) {
            log.info("소셜 토큰이 없어 연동 해지를 스킵합니다.");
            return;
        }

        if (provider == AuthProvider.KAKAO) {
            try {
                postWithBearerToken(kakaoUnlinkUri, token);
                log.info("카카오 서비스 연동 해지(Unlink) 성공.");
            } catch (RestClientException e) {
                log.error("카카오 서비스 연동 해지(Unlink) 실패: {}", e.getMessage());
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }
        } else if (provider == AuthProvider.GOOGLE) {
            revokeSocialToken(provider, token);
        }
    }

    /**
     * Bearer 토큰 인증 헤더를 사용하는 공통 HTTP GET 요청 헬퍼 메서드.
     */
    private Map<String, Object> fetchAttributesWithBearerToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    /**
     * Bearer 토큰 인증 헤더를 사용하는 공통 HTTP POST 요청 헬퍼 메서드.
     */
    private void postWithBearerToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    /**
     * JWT ID Token 여부 확인 (Header.Payload.Signature 구조 형태)
     */
    private boolean isIdToken(String token) {
        return token.startsWith("eyJ") && token.contains(".");
    }
}
