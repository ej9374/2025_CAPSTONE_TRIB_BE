package triB.triB.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import triB.triB.room.entity.Room;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "trips",
        indexes = {
                @Index(name = "idx_trips_destination", columnList = "destination"),
                @Index(name = "idx_trips_room_id", columnList = "room_id")
        }
)
@Where(clause = "version_status = 'NEW'")
public class Trip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;
    
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "room_id", insertable = false, updatable = false)
   private Room room;

    @Column(name = "destination", nullable = false)
    private String destination;

    @Builder.Default
    @Column(name = "version_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VersionStatus versionStatus = VersionStatus.NEW;

    @Builder.Default
    @Column(name = "trip_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TripStatus tripStatus = TripStatus.READY;
    
    @Builder.Default
    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked = false;

    @Column(name = "travel_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private TravelMode travelMode;

    @Column(name = "budget", nullable = true)
    private Integer budget;

    @Column(name = "accommodation_cost_info", nullable = true)
    private String accommodationCostInfo;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
