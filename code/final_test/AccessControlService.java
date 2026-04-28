import java.util.List;

public class AccessControlService {
    public int countAlerts(List<AccessEvent> events) {
        int alerts = 0;
        for (AccessEvent event : events) {
            if (event == null) {
                continue;
            }
            if (event.forcedEntry() || event.hour() < 6 || event.hour() > 23) {
                alerts++;
            }
        }
        return alerts;
    }

    public String classifyZone(AccessEvent event) {
        if (event == null) {
            return "UNKNOWN";
        }
        if (event.forcedEntry()) {
            return "CRITICAL";
        }
        return switch (event.building()) {
            case "LAB", "FINANCE" -> "HIGH";
            case "LIBRARY", "DORM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    public boolean shouldNotify(AccessEvent event) {
        return event != null && (event.forcedEntry() || event.hour() < 6 || event.hour() >= 23);
    }
}
