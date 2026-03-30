package learntime.backend.domain.user.controller;

import jakarta.persistence.EntityManager;
import learntime.backend.domain.user.model.RefreshToken;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        // 테스트 실행 전, 미리 검증용 사용자 데이터를 DB에 세팅합니다.
        // 실무 최적화: 비밀번호는 반드시 PasswordEncoder로 암호화하여 저장해야 실제 로그인 검증 로직을 통과할 수 있습니다.
        User testUser = User.builder()
                .email("test123@example.com")
                .password(passwordEncoder.encode("learntime123!!!!"))
                .name("TestUser")
                .role(User.Role.ROLE_USER)
                .build();

        userRepository.save(testUser);
    }

    @Test
    @DisplayName("유효한 이메일과 비밀번호로 로그인하면 AccessToken과 RefreshToken 쿠키를 발급한다.")
    void login_Success() throws Exception {
        // given: JDK 21 텍스트 블록을 활용하여 JSON 객체 생성 (ObjectMapper 직렬화 비용 절감)
        // 핵심 수정: setUp()에서 저장한 유저 정보와 요청 JSON의 데이터가 정확히 일치해야 합니다.
        String requestBody = """
                {
                    "email": "test123@example.com",
                    "password": "learntime123!!!!"
                }
                """;

        // when: 로그인 API 엔드포인트로 POST 요청을 보냅니다.
        ResultActions resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON));

        // then: HTTP 상태 코드 200과 함께 토큰 발급 여부를 검증합니다.
        resultActions
                .andDo(print()) // 콘솔에 Request/Response 전체를 출력하여 디버깅을 돕습니다.
                .andExpect(status().isOk())
                // AccessToken이 응답 Body의 accessToken 필드에 존재하는지 확인
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // RefreshToken이 쿠키에 정상적으로 담겼는지, HttpOnly 및 Secure(HTTPS) 설정이 들어갔는지 확인
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true));
    }

    @Test
    @DisplayName("유저를 삭제하면 User는 소프트 삭제(Update)되고, 연관된 RefreshToken은 하드 삭제(Delete)된다.")
    void deleteUser_ShouldSoftDeleteUser_AndHardDeleteRefreshToken() {
        // given: 영속성 컨텍스트(Heap 영역 1차 캐시)에 User와 RefreshToken을 생성 및 영속화합니다.
        User user = User.builder()
                .email("cascade_test@example.com")
                .password(passwordEncoder.encode("learntime123!!!!"))
                .name("CascadeUser")
                .role(User.Role.ROLE_USER)
                .build();

        userRepository.save(user); // User 영속화 (ID 할당)

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("sample-refresh-token-data")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        em.persist(refreshToken);
        em.flush();
        em.clear(); // 1차 캐시를 비워 이후 쿼리가 실제 RDBMS 네트워크를 타도록 유도

        // when: 유저를 조회하여 삭제합니다. (CascadeType.ALL 및 orphanRemoval 동작 유도)
        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        userRepository.delete(savedUser);

        em.flush();
        em.clear();

        // then:
        // 1. RefreshToken은 식별 관계를 가지며 하드 삭제되어야 하므로 DB에 존재해선 안 됩니다. (시간 복잡도 O(1) PK 조회)
        RefreshToken deletedToken = em.find(RefreshToken.class, savedUser.getUserId());
        assertThat(deletedToken).isNull();

        // 2. User는 @SQLRestriction에 의해 애플리케이션 레벨(Spring Data JPA)에서는 삭제된 것으로 취급되어 조회되지 않아야 합니다.
        boolean userExists = userRepository.existsById(savedUser.getUserId());
        assertThat(userExists).isFalse();

        // 3. 실제 RDBMS 레코드 검증: User 레코드는 삭제되지 않고 deleted_at 필드만 업데이트(소프트 삭제) 되어야 합니다.
        // @SQLDelete에 명시된 테이블명(users) 기준으로 Native Query를 실행하여 확인합니다.
        Object deletedAt = em.createNativeQuery("SELECT deleted_at FROM users WHERE user_id = :id")
                .setParameter("id", savedUser.getUserId())
                .getSingleResult();

        // 1970-01-01 초기값이 아닌, 실제 삭제 시간이 기록되었는지 검증
        assertThat(deletedAt.toString()).isNotEqualTo("1970-01-01 00:00:00.0");
    }


}