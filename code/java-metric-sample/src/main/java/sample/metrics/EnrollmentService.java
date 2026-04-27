package sample.metrics;

public class EnrollmentService {

    private Course course;
    private Advisor advisor;
    private ScoreStrategy scoreStrategy;

    public EnrollmentService(Course course, Advisor advisor, ScoreStrategy scoreStrategy) {
        this.course = course;
        this.advisor = advisor;
        this.scoreStrategy = scoreStrategy;
    }

    public boolean enroll(Student student, int currentCount, boolean prerequisitesPassed) {
        if (student == null) {
            return false;
        }
        if (!prerequisitesPassed) {
            student.markWarning();
            return false;
        }
        if (course.isFull(currentCount)) {
            advisor.notifyStudent(student);
            return false;
        }
        advisor.acceptStudent(student);
        student.assignAdvisor(advisor);
        return true;
    }

    public String review(Student student, int score) {
        if (score >= 90) {
            student.addCredits(3);
            return "A";
        }
        if (score >= 75) {
            student.addCredits(2);
            return "B";
        }
        if (score >= 60) {
            student.addCredits(1);
            return "C";
        }
        student.markWarning();
        return "D";
    }

    public String determineRisk(int score) {
        switch (score / 10) {
            case 10:
            case 9:
                return "LOW";
            case 8:
            case 7:
                return "MEDIUM";
            default:
                return "HIGH";
        }
    }

    public int calculateAverage(int[] scores) {
        if (scores == null || scores.length == 0) {
            return 0;
        }
        int total = 0;
        for (int score : scores) {
            total += scoreStrategy.normalize(score);
        }
        return total / scores.length;
    }
}
