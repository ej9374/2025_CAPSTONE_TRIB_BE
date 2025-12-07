package triB.triB.schedule.event;

public record ScheduleBatchUpdatedEvent(
        Long tripId,
        Long roomId,
        Long userId,
        String nickname,
        Integer dayNumber
) {}
