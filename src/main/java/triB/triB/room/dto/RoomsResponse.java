package triB.triB.room.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class RoomsResponse {
    private Long roomId;
    private String roomName;
    private List<String> photoUrls;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String content;
    private LocalDateTime createdAt;
    private Integer messageNum;
    private Integer people;
}
