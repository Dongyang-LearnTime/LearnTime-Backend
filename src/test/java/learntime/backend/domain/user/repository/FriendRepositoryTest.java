package learntime.backend.domain.user.repository;

import learntime.backend.domain.user.model.Friend;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class FriendRepositoryTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testExistsFriendship() {
        User u1 = new User();
        ReflectionTestUtils.setField(u1, "email", "u1@test.com");
        ReflectionTestUtils.setField(u1, "name", "u1");
        
        User u2 = new User();
        ReflectionTestUtils.setField(u2, "email", "u2@test.com");
        ReflectionTestUtils.setField(u2, "name", "u2");

        userRepository.save(u1);
        userRepository.save(u2);

        Friend friend = Friend.builder()
                .user(u1)
                .friendUser(u2)
                .build();
        friendRepository.save(friend);

        boolean exists1 = friendRepository.existsFriendship(u1.getUserId(), u2.getUserId());
        boolean exists2 = friendRepository.existsFriendship(u2.getUserId(), u1.getUserId());

        assertThat(exists1).isTrue();
        assertThat(exists2).isTrue();
    }
}