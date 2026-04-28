public class InternshipPlan {
    private final String studentId;
    private final String company;
    private int weeklyLogs;
    private boolean enterpriseBound;

    public InternshipPlan(String studentId, String company) {
        this.studentId = studentId;
        this.company = company;
    }

    public void addWeeklyLog() {
        weeklyLogs++;
    }

    public void bindEnterprise() {
        enterpriseBound = true;
    }

    public boolean isHealthy() {
        return enterpriseBound && weeklyLogs >= 4;
    }
}
