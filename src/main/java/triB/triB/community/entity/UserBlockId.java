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
public class UserBlockId implements Serializable {

    @Column(name = "blocker_user_id")
    private Long blockerUserId;  // 차단한 사람

    @Column(name = "blocked_user_id")
    private Long blockedUserId;  // 차단당한 사람
}
