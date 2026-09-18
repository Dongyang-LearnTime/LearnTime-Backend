package learntime.backend.domain.message.converter;

import learntime.backend.domain.message.dto.response.MessageResponseDTO;
import learntime.backend.domain.message.model.Message;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class MessageConverter {

    public MessageConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static MessageResponseDTO toMessageResponse(Message message) {
        return MessageResponseDTO.builder()
                .messageId(message.getMessageId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .senderId(message.getSender() == null ? null : message.getSender().getUserId())
                .senderName(message.getSender() == null ? "탈퇴한 사용자" : message.getSender().getName())
                .senderRole(message.getSender() == null ? null : message.getSender().getRole())
                .receiverId(message.getReceiver() == null ? null : message.getReceiver().getUserId())
                .receiverName(message.getReceiver() == null ? "탈퇴한 사용자" : message.getReceiver().getName())
                .build();
    }
}
