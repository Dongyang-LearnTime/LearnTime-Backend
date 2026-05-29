package learntime.backend.domain.message.service;

import learntime.backend.domain.message.converter.MessageConverter;
import learntime.backend.domain.message.dto.request.MessageRequestDTO;
import learntime.backend.domain.message.dto.response.MessageResponseDTO;
import learntime.backend.domain.message.error.code.MessageErrorCode;
import learntime.backend.domain.message.error.exception.MessageException;
import learntime.backend.domain.message.event.MessageSentEvent;
import learntime.backend.domain.message.model.Message;
import learntime.backend.domain.message.repository.MessageRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.utils.UserBlockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final UserBlockUtil userBlockUtil;

    // 쪽지 보내기
    @Transactional
    public Long sendMessage(Long senderId, MessageRequestDTO request) {
        if (senderId.equals(request.receiverId())) {
            throw new MessageException(MessageErrorCode.CANNOT_SEND_SELF);
        }

        User sender = getUser(senderId);
        User receiver = getUser(request.receiverId());

        userBlockUtil.validateNotBlockedByUser(senderId, request.receiverId()); // 차단 당했는지 확인

        Message message = Message.builder()
                .content(request.content())
                .sender(sender)
                .receiver(receiver)
                .build();

        Message savedMessage = messageRepository.save(message);

        log.info("[쪽지 전송 완료] messageId={}, senderId={}, receiverId={}",
                savedMessage.getMessageId(), senderId, request.receiverId());

        // 쪽지 수신 알림 이벤트 발행
        eventPublisher.publishEvent(new MessageSentEvent(
                savedMessage.getMessageId(),
                sender.getUserId(),
                sender.getName(),
                receiver.getUserId()
        ));

        return savedMessage.getMessageId();
    }

    // 보낸 쪽지 목록 조회 (오프셋 페이징)
    public Page<MessageResponseDTO> getSentMessages(Long senderId, Pageable pageable) {
        Page<Message> messages = messageRepository.findSentMessages(senderId, pageable);
        return messages.map(MessageConverter::toMessageResponse);
    }

    // 받은 쪽지 목록 조회 (오프셋 페이징)
    public Page<MessageResponseDTO> getReceivedMessages(Long receiverId, Pageable pageable) {
        Page<Message> messages = messageRepository.findReceivedMessages(receiverId, pageable);
        return messages.map(MessageConverter::toMessageResponse);
    }

    // 쪽지 상세 조회
    public MessageResponseDTO getMessage(Long userId, Long messageId) {
        Message message = getMessageOrThrow(messageId);

        validateMessageAccess(message, userId);

        return MessageConverter.toMessageResponse(message);
    }

    //쪽지 일괄 읽음 처리
    @Transactional
    public void readMessages(Long userId, java.util.List<Long> messageIds) {
        messageRepository.markAsRead(messageIds, userId, LocalDateTime.now());
    }

    // 쪽지 삭제 (수신측 혹은 송신측 삭제 여부 true 설정)
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = getMessageOrThrow(messageId);

        validateMessageAccess(message, userId);

        if (message.getSender().getUserId().equals(userId)) {
            message.deleteBySender();
            log.info("[쪽지 삭제 처리 - 송신자] messageId={}, senderId={}", messageId, userId);
        }

        if (message.getReceiver().getUserId().equals(userId)) {
            message.deleteByReceiver();
            log.info("[쪽지 삭제 처리 - 수신자] messageId={}, receiverId={}", messageId, userId);
        }
    }


    private void validateMessageAccess(Message message, Long userId) {
        // 현재 사용자가 송신자인지 확인
        boolean isSender = message.getSender().getUserId().equals(userId);

        // 현재 사용자가 수신자인지 확인
        boolean isReceiver = message.getReceiver().getUserId().equals(userId);

        // 송신자/수신자 모두 아닌 경우 접근 불가
        if (!isSender && !isReceiver) {
            throw new MessageException(MessageErrorCode.ACCESS_DENIED);
        }

        // 송신자이지만 이미 삭제한 쪽지인 경우 접근 불가
        if (isSender && message.isSenderDeleted()) {
            throw new MessageException(MessageErrorCode.ACCESS_DENIED);
        }

        // 수신자이지만 이미 삭제한 쪽지인 경우 접근 불가
        if (isReceiver && message.isReceiverDeleted()) {
            throw new MessageException(MessageErrorCode.ACCESS_DENIED);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }

    private Message getMessageOrThrow(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException(MessageErrorCode.MESSAGE_NOT_FOUND));
    }

}
