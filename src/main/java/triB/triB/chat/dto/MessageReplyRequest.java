package triB.triB.chat.dto;

import lombok.Getter;

@Getter
public class MessageReplyRequest {
    private String content;
    private Long messageId;
}
