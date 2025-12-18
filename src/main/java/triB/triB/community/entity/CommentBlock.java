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
        name = "comment_blocks",
        indexes = {
                @Index(name = "idx_comment_blocks_blocker", columnList = "blocker_user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_blocker_comment",
                        columnNames = {"blocker_user_id", "blocked_comment_id"})
        }
)
public class CommentBlock {

    @EmbeddedId
    private CommentBlockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id", insertable = false, updatable = false)
    private User blockerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_comment_id", insertable = false, updatable = false)
    private Comment blockedComment;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
