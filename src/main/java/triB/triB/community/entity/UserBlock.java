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
        name = "user_blocks",
        indexes = {
                @Index(name = "idx_user_blocks_blocker", columnList = "blocker_user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_blocker_blocked",
                        columnNames = {"blocker_user_id", "blocked_user_id"})
        }
)
public class UserBlock {

    @EmbeddedId
    private UserBlockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id", insertable = false, updatable = false)
    private User blockerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", insertable = false, updatable = false)
    private User blockedUser;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
