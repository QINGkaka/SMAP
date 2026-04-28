import java.util.List;

public class InternshipService {
    public int evaluatePlans(List<InternshipPlan> plans) {
        int healthy = 0;
        for (InternshipPlan plan : plans) {
            if (plan != null && plan.isHealthy()) {
                healthy++;
            }
        }
        return healthy;
    }

    public String classifyPlan(InternshipPlan plan, int mentorMeetings, boolean weeklySummaryReady) {
        if (plan == null) {
            return "UNKNOWN";
        }
        if (plan.isHealthy() && mentorMeetings >= 3 && weeklySummaryReady) {
            return "STABLE";
        }
        if (mentorMeetings >= 2 || weeklySummaryReady) {
            return "WATCH";
        }
        return "RISK";
    }
}
