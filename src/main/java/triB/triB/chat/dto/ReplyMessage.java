package triB.triB.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReplyMessage {
    private Long messageId;
    private String content;
}
