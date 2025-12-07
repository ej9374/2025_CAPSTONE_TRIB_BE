package triB.triB.schedule.entity;

public enum TripFilterType {
    FUTURE("미래 여행"),
    PAST("과거 여행");

    private final String description;

    TripFilterType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
