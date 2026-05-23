package learntime.backend.domain.message.service;

import learntime.backend.domain.message.dto.request.MessageRequestDTO;
import learntime.backend.domain.message.dto.response.MessageResponseDTO;
import learntime.backend.domain.message.error.code.MessageErrorCode;
import learntime.backend.domain.message.error.exception.MessageException;
import learntime.backend.domain.message.event.MessageSentEvent;
import learntime.backend.domain.message.model.Message;
import learntime.backend.domain.message.repository.MessageRepository;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@RecordApplicationEvents
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private learntime.backend.domain.user.service.UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private learntime.backend.domain.study.scheduler.CacheCleanupScheduler cacheCleanupScheduler;

    @Autowired
    private ApplicationEvents applicationEvents;

    private User createUser(String name, String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password("password123!")
                        .name(name)
                        .socialId(email)
                        .socialProvider(AuthProvider.LOCAL)
                        .role(Role.ROLE_USER)
                        .build()
        );
    }

    @Test
    @DisplayName("쪽지 전송 성공 및 이벤트 발행 검증")
    void sendMessage_success() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        MessageRequestDTO request = new MessageRequestDTO(receiver.getUserId(), "안녕하세요.");

        // when
        Long messageId = messageService.sendMessage(sender.getUserId(), request);

        // then
        Optional<Message> foundMessage = messageRepository.findById(messageId);
        assertThat(foundMessage).isPresent();
        assertThat(foundMessage.get().getContent()).isEqualTo("안녕하세요.");
        assertThat(foundMessage.get().getSender().getUserId()).isEqualTo(sender.getUserId());
        assertThat(foundMessage.get().getReceiver().getUserId()).isEqualTo(receiver.getUserId());

        // 이벤트 발행 검증
        long count = applicationEvents.stream(MessageSentEvent.class)
                .filter(event -> event.messageId().equals(messageId))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("자기 자신에게 쪽지 전송 불가")
    void sendMessage_cannotSendToSelf() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        MessageRequestDTO request = new MessageRequestDTO(sender.getUserId(), "나에게 보내기");

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage(sender.getUserId(), request))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining(MessageErrorCode.CANNOT_SEND_SELF.getMessage());
    }

    @Test
    @DisplayName("보낸 쪽지 목록 조회 페이징 및 삭제 필터링 검증")
    void getSentMessages_success() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");

        Message msg1 = messageRepository.save(new Message("내용1", sender, receiver));
        Message msg2 = messageRepository.save(new Message("내용2", sender, receiver));
        Message msg3 = messageRepository.save(new Message("내용3", sender, receiver));

        // msg3는 송신자 측에서 삭제 처리
        msg3.deleteBySender();
        messageRepository.save(msg3);

        // when
        Page<MessageResponseDTO> page = messageService.getSentMessages(sender.getUserId(), PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(2); // 삭제 안 된 msg1, msg2만 조회되어야 함
        assertThat(page.getContent()).extracting("content")
                .containsExactlyInAnyOrder("내용1", "내용2");
    }

    @Test
    @DisplayName("받은 쪽지 목록 조회 페이징 및 삭제 필터링 검증")
    void getReceivedMessages_success() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");

        Message msg1 = messageRepository.save(new Message("내용1", sender, receiver));
        Message msg2 = messageRepository.save(new Message("내용2", sender, receiver));

        // msg2는 수신자 측에서 삭제 처리
        msg2.deleteByReceiver();
        messageRepository.save(msg2);

        // when
        Page<MessageResponseDTO> page = messageService.getReceivedMessages(receiver.getUserId(), PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(1); // 삭제 안 된 msg1만 조회되어야 함
        assertThat(page.getContent().get(0).content()).isEqualTo("내용1");
    }

    @Test
    @DisplayName("쪽지 상세 조회시 읽음 처리 되지 않음")
    void getMessage_shouldNotMarkAsRead() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        Message message = messageRepository.save(new Message("내용", sender, receiver));

        // when
        MessageResponseDTO response = messageService.getMessage(receiver.getUserId(), message.getMessageId());

        // then
        assertThat(response.readAt()).isNull();

        // DB 반영 확인
        Message updatedMessage = messageRepository.findById(message.getMessageId()).get();
        assertThat(updatedMessage.getReadAt()).isNull();
    }

    @Test
    @DisplayName("쪽지 여러 개 일괄 읽음 처리 성공")
    void readMessages_success() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        Message message1 = messageRepository.save(new Message("내용1", sender, receiver));
        Message message2 = messageRepository.save(new Message("내용2", sender, receiver));
        Message message3 = messageRepository.save(new Message("내용3", sender, receiver));

        // when (1번과 2번만 읽음 처리)
        messageService.readMessages(receiver.getUserId(), java.util.List.of(message1.getMessageId(), message2.getMessageId()));

        // then
        Message updated1 = messageRepository.findById(message1.getMessageId()).get();
        Message updated2 = messageRepository.findById(message2.getMessageId()).get();
        Message updated3 = messageRepository.findById(message3.getMessageId()).get();

        assertThat(updated1.getReadAt()).isNotNull();
        assertThat(updated2.getReadAt()).isNotNull();
        assertThat(updated3.getReadAt()).isNull(); // 3번은 안 읽었으므로 null
    }

    @Test
    @DisplayName("관련 없는 사용자가 쪽지 조회 시 에러 발생")
    void getMessage_accessDenied() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        User other = createUser("제3자", "other@test.com");
        Message message = messageRepository.save(new Message("내용", sender, receiver));

        // when & then
        assertThatThrownBy(() -> messageService.getMessage(other.getUserId(), message.getMessageId()))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining(MessageErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("송신자만 삭제한 경우 DB에 남아있고, 송신자 측에서는 조회 안 됨")
    void deleteMessage_bySenderOnly() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        Message message = messageRepository.save(new Message("내용", sender, receiver));

        // when
        messageService.deleteMessage(sender.getUserId(), message.getMessageId());

        // then
        Optional<Message> foundMessage = messageRepository.findById(message.getMessageId());
        assertThat(foundMessage).isPresent();
        assertThat(foundMessage.get().isSenderDeleted()).isTrue();
        assertThat(foundMessage.get().isReceiverDeleted()).isFalse();

        // 송신자 조회 시 에러
        assertThatThrownBy(() -> messageService.getMessage(sender.getUserId(), message.getMessageId()))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining(MessageErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("송신자와 수신자 둘 다 삭제한 경우 바로 삭제되지 않고, 읽은 지 1개월이 지나 스케줄러가 실행되어야 물리 삭제됨")
    void deleteMessage_byBoth_shouldNotDeleteImmediatelyAndGetsCleanedByScheduler() throws Exception {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        Message message = messageRepository.save(new Message("내용", sender, receiver));

        // when 1: 둘 다 삭제
        messageService.deleteMessage(sender.getUserId(), message.getMessageId());
        messageService.deleteMessage(receiver.getUserId(), message.getMessageId());

        // then 1: DB에는 아직 남아 있어야 함 (읽음 처리가 되지 않았음)
        Optional<Message> foundBeforeRead = messageRepository.findById(message.getMessageId());
        assertThat(foundBeforeRead).isPresent();

        // when 2: 읽음 처리 후 1개월하고 1일 전으로 readAt 수정
        message.readMessage();
        java.lang.reflect.Field readAtField = Message.class.getDeclaredField("readAt");
        readAtField.setAccessible(true);
        readAtField.set(message, java.time.LocalDateTime.now().minusMonths(1).minusDays(1));
        messageRepository.saveAndFlush(message);

        // 스케줄러 실행
        cacheCleanupScheduler.deleteExpiredMessages();

        // then 2: DB에서 정상적으로 물리 삭제됨
        Optional<Message> foundAfterCleanup = messageRepository.findById(message.getMessageId());
        assertThat(foundAfterCleanup).isEmpty();
    }

    @Test
    @DisplayName("사용자 탈퇴 시 해당 사용자가 보낸 쪽지와 받은 쪽지가 soft-delete 처리됨")
    void deleteUser_shouldSoftDeleteMessages() {
        // given
        User sender = createUser("송신자", "sender@test.com");
        User receiver = createUser("수신자", "receiver@test.com");
        Message message = messageRepository.save(new Message("내용", sender, receiver));

        // when: 송신자 탈퇴
        userService.deleteUser(sender.getEmail());

        // then: 송신자 삭제 여부만 true로 변경되어야 함
        Message updatedMessage = messageRepository.findById(message.getMessageId()).get();
        assertThat(updatedMessage.isSenderDeleted()).isTrue();
        assertThat(updatedMessage.isReceiverDeleted()).isFalse();
    }
}
