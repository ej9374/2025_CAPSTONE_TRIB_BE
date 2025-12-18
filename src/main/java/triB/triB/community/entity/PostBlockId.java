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
public class PostBlockId implements Serializable {

    @Column(name = "blocker_user_id")
    private Long blockerUserId;  // 차단한 사람

    @Column(name = "blocked_post_id")
    private Long blockedPostId;  // 차단된 게시글
}
