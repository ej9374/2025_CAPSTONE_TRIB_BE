package triB.triB.chat.event;


public record TripCreatedEvent(
        Long tripId,
        Long roomId
) {
}
