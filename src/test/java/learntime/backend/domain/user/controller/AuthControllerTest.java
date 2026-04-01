package learntime.backend.domain.user.controller;

import jakarta.persistence.EntityManager;
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

}