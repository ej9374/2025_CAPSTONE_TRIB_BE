package triB.triB.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class TripCreateStatusResponse {
    private TripCreateStatus tripCreateStatus;
    private Long tripId;
}