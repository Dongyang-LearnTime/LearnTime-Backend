package learntime.backend.domain.notification.service;

import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.model.Notification;
import learntime.backend.domain.notification.repository.NotificationRepository;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("알림 생성 로직이 repository.save()를 정상적으로 호출한다")
    void notify_SavesToDatabase() {
        // given
        Long receiverId = 1L;
        User receiver = User.builder()
                .email("test@example.com")
                .socialProvider(AuthProvider.LOCAL)
                .password("password")
                .name("testUser")
                .role(Role.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(receiver, "userId", receiverId);

        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "notificationId", 100L);
            return saved;
        });

        // when
        notificationService.notify(
                receiverId,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "친구 요청",
                "테스트 알림 메시지",
                2L,
                "FRIEND_REQUEST"
        );

        // then
        verify(userRepository, times(1)).findById(receiverId);
        verify(notificationRepository, times(1)).save(argThat(notification -> 
                notification.getTitle().equals("친구 요청") &&
                notification.getMessage().equals("테스트 알림 메시지") &&
                notification.getReceiver().getUserId().equals(receiverId) &&
                notification.getType() == NotificationType.FRIEND_REQUEST_RECEIVED &&
                notification.getReferenceId().equals(2L) &&
                notification.getReferenceType().equals("FRIEND_REQUEST")
        ));
    }
}
