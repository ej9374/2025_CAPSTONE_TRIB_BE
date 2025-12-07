package triB.triB.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.units.qual.C;
import org.hibernate.annotations.CreationTimestamp;
import triB.triB.room.entity.Room;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "message_places",
        indexes = {
                @Index(name = "idx_room_id", columnList = "room_id"),
                @Index(name = "idx_message_id", columnList = "message_id")
        }
)
public class MessagePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_place_id")
    private Long messagePlaceId;

    @JoinColumn(name = "message_id", nullable = false)
    @OneToOne
    private Message message;

    @JoinColumn(name = "room_id", nullable = false)
    @ManyToOne
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_tag", nullable = false)
    private PlaceTag placeTag;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
