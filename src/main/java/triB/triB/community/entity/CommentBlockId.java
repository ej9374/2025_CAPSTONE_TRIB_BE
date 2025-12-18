package triB.triB.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommentBlockId implements Serializable {

    @Column(name = "blocker_user_id")
    private Long blockerUserId;  // 차단한 사람

    @Column(name = "blocked_comment_id")
    private Long blockedCommentId;  // 차단된 댓글
}
