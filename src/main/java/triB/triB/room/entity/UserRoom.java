package triB.triB.room.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import triB.triB.auth.entity.User;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "user_room",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_room_id", columnList = "room_id")
        }
)
@Where(clause = "room_status = 'ACTIVE'")
public class UserRoom {

    @EmbeddedId
    private UserRoomId id;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    @MapsId("userId")
    private User user;

    @JoinColumn(name = "room_id", nullable = false)
    @ManyToOne
    @MapsId("roomId")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false)
    private RoomStatus roomStatus = RoomStatus.ACTIVE;
}
