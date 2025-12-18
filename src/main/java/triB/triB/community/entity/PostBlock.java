package triB.triB.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import triB.triB.auth.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "post_blocks",
        indexes = {
                @Index(name = "idx_post_blocks_blocker", columnList = "blocker_user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_blocker_post",
                        columnNames = {"blocker_user_id", "blocked_post_id"})
        }
)
public class PostBlock {

    @EmbeddedId
    private PostBlockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id", insertable = false, updatable = false)
    private User blockerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_post_id", insertable = false, updatable = false)
    private Post blockedPost;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
