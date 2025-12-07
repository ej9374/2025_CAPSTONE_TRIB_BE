package triB.triB.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import triB.triB.chat.entity.MessageStatus;
import triB.triB.chat.entity.MessageType;
import triB.triB.chat.entity.PlaceTag;

@Getter
@Setter
@Builder
public class MessageDto {
    private Long messageId;
    private String content;
    private MessageType messageType;
    private MessageStatus messageStatus;
    private PlaceTag tag;
    private Boolean isBookmarked;
    private PlaceDetail placeDetail;
    private CommunityDetail communityDetail;
    private ReplyMessage replyMessage;
}
