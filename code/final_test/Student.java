public class Student extends Person {
    private int credits;
    private double gpa;
    private int warnings;
    private String advisorId;

    public Student(String id, String name, int credits, double gpa) {
        super(id, name);
        this.credits = credits;
        this.gpa = gpa;
    }

    public void addCredits(int delta) {
        if (delta > 0) {
            credits += delta;
        }
    }

    public void applyScore(double score) {
        if (score >= 95) {
            gpa = Math.min(4.0, gpa + 0.2);
        } else if (score >= 85) {
            gpa = Math.min(4.0, gpa + 0.1);
        } else if (score < 60) {
            warnings++;
            gpa = Math.max(0, gpa - 0.3);
        }
    }

    public void markWarning(String reason) {
        if (reason != null && !reason.isBlank()) {
            warnings++;
        }
    }

    public boolean isAtRisk() {
        return warnings >= 2 || gpa < 2.0 || credits < 20;
    }

    public String statusLevel() {
        return switch (warnings) {
            case 0 -> "NORMAL";
            case 1 -> "WATCH";
            case 2 -> "RISK";
            default -> "CRITICAL";
        };
    }

    public int credits() {
        return credits;
    }

    public double gpa() {
        return gpa;
    }

    public int warnings() {
        return warnings;
    }

    public void setAdvisorId(String advisorId) {
        this.advisorId = advisorId;
    }

    public String advisorId() {
        return advisorId;
    }
}
