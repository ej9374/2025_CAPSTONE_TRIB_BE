package triB.triB.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import triB.triB.chat.entity.PlaceTag;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", insertable = false, updatable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "place_tag", nullable = false)
    private PlaceTag placeTag;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "is_visit", nullable = false)
    private Boolean isVisit;

    @Column(name = "arrival", nullable = false)
    private LocalDateTime arrival;

    @Column(name = "departure", nullable = false)
    private LocalDateTime departure;

    @Column(name = "travel_time", nullable = true)
    private String travelTime;

    @Column(name = "estimated_cost", nullable = true)
    private Integer estimatedCost;

    @Column(name = "cost_explanation", nullable = true)
    private String costExplanation;
}
