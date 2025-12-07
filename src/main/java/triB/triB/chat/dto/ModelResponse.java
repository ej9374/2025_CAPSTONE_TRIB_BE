package triB.triB.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import triB.triB.chat.entity.PlaceTag;
import triB.triB.schedule.entity.TravelMode;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ModelResponse {
    private Integer budget;
    private TravelMode travelMode;
    private String accommodationCostInfo;
    private List<Itinerary> itinerary;

    @Getter
    @Setter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Itinerary {
        private Integer day;
        private List<Visit> visits;
    }

    @Getter
    @Setter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Visit {
        private Integer order;
        private String displayName;
        private PlaceTag placeTag;
        private Double latitude;
        private Double longitude;
        private String arrival;
        private String departure;
        private Integer travelTime;
        private Integer estimatedCost;
        private String costExplanation;
    }
}
