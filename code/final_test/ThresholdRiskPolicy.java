public class ThresholdRiskPolicy implements RiskPolicy {
    @Override
    public int evaluate(Student student) {
        if (student == null) {
            return 0;
        }
        if (student.isAtRisk() && student.gpa() < 1.5) {
            return 4;
        }
        if (student.isAtRisk()) {
            return 3;
        }
        if (student.gpa() < 2.5 || student.warnings() > 0) {
            return 2;
        }
        return 1;
    }
}
