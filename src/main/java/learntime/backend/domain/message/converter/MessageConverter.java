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
                .senderId(message.getSender().getUserId())
                .senderName(message.getSender().getName())
                .receiverId(message.getReceiver().getUserId())
                .receiverName(message.getReceiver().getName())
                .build();
    }
}
