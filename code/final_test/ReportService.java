import java.util.List;

public class ReportService {
    public int countHighRiskStudents(List<Student> students, RiskPolicy riskPolicy) {
        int highRisk = 0;
        for (Student student : students) {
            if (student != null && riskPolicy.evaluate(student) >= 3) {
                highRisk++;
            }
        }
        return highRisk;
    }

    public String summarizeFinance(double totalPaid, int rejectedOrders, int overdueOrders) {
        if (rejectedOrders > overdueOrders && rejectedOrders >= 3) {
            return "REJECTED_HEAVY";
        }
        if (overdueOrders >= 5 || totalPaid < 10000) {
            return "WATCH";
        }
        return "STABLE";
    }
}
