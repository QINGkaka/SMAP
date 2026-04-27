package sample.metrics;

public class Student extends Person {

    private int credits;
    private int warningCount;
    private Advisor advisor;

    public Student(String id, String name, Advisor advisor) {
        super(id, name);
        this.advisor = advisor;
        this.credits = 0;
        this.warningCount = 0;
    }

    public void addCredits(int value) {
        if (value > 0) {
            credits += value;
        }
    }

    public void markWarning() {
        warningCount++;
        if (advisor != null) {
            advisor.notifyStudent(this);
        }
    }

    public boolean isAtRisk() {
        return warningCount >= 2 || credits < 12;
    }

    public void assignAdvisor(Advisor nextAdvisor) {
        this.advisor = nextAdvisor;
    }
}
