package triB.triB.chat.event;

import triB.triB.chat.entity.Message;
import triB.triB.chat.entity.MessageType;

public record ChatMessageCreatedEvent(
        Long roomId,
        Long userId,
        String nickname,
        String photoUrl,
        String content,
        MessageType messageType
) {}
