import java.util.List;

public class EnrollmentService {
    private final StudentRepository repository;
    private final ScorePolicy scorePolicy;
    private final RiskPolicy riskPolicy;
    private final NotificationGateway notificationGateway;

    public EnrollmentService(StudentRepository repository,
                             ScorePolicy scorePolicy,
                             RiskPolicy riskPolicy,
                             NotificationGateway notificationGateway) {
        this.repository = repository;
        this.scorePolicy = scorePolicy;
        this.riskPolicy = riskPolicy;
        this.notificationGateway = notificationGateway;
    }

    public EnrollmentRecord enroll(String studentId, String courseCode, int currentSize, boolean hasConflict, boolean accountFrozen) {
        Student student = repository.findStudent(studentId);
        Course course = repository.findCourse(courseCode);
        if (student == null || course == null) {
            return null;
        }
        EnrollmentRecord record = new EnrollmentRecord(studentId, courseCode);
        if (!course.canEnroll(currentSize, hasConflict, accountFrozen)) {
            record.reject();
            return record;
        }
        if (student.isAtRisk() && course.credits() > 3) {
            record.setPriority(3);
        } else if (student.gpa() >= 3.5) {
            record.setPriority(1);
        } else {
            record.setPriority(2);
        }
        record.approve();
        repository.addEnrollment(record);
        return record;
    }

    public int reviewPerformance(String studentId, List<Integer> scores) {
        Student student = repository.findStudent(studentId);
        if (student == null) {
            return 0;
        }
        double average = scorePolicy.calculate(scores);
        student.applyScore(average);
        return riskPolicy.evaluate(student);
    }

    public double calculateAverage(List<Integer> scores) {
        double total = 0;
        int valid = 0;
        for (Integer score : scores) {
            if (score == null || score < 0) {
                continue;
            }
            total += score;
            valid++;
        }
        return valid == 0 ? 0 : total / valid;
    }

    public int determinePriority(Student student) {
        int risk = riskPolicy.evaluate(student);
        return switch (risk) {
            case 4 -> 10;
            case 3 -> 8;
            case 2 -> 6;
            default -> 4;
        };
    }

    public boolean notifyStudent(Student student, String message) {
        return student != null && notificationGateway.send(student.id(), message, student.isAtRisk());
    }
}
