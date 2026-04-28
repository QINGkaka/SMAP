public class AccessEvent {
    private final String building;
    private final int hour;
    private final boolean forcedEntry;

    public AccessEvent(String building, int hour, boolean forcedEntry) {
        this.building = building;
        this.hour = hour;
        this.forcedEntry = forcedEntry;
    }

    public String building() {
        return building;
    }

    public int hour() {
        return hour;
    }

    public boolean forcedEntry() {
        return forcedEntry;
    }
}
