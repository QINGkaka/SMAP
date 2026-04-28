public class EnrollmentRecord {
    private final String studentId;
    private final String courseCode;
    private String state;
    private int priority;

    public EnrollmentRecord(String studentId, String courseCode) {
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.state = "PENDING";
        this.priority = 1;
    }

    public void approve() {
        state = "APPROVED";
    }

    public void reject() {
        state = "REJECTED";
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String studentId() {
        return studentId;
    }

    public String courseCode() {
        return courseCode;
    }

    public String state() {
        return state;
    }

    public int priority() {
        return priority;
    }
}
