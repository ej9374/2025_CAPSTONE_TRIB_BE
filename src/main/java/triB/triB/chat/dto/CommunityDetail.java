package triB.triB.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CommunityDetail {
    private Long postId;
    private String title;
    private String photo;
}
