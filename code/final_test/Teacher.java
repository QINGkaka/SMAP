import java.util.ArrayList;
import java.util.List;

public class Teacher extends Person {
    private final String department;
    private final List<String> adviseeIds = new ArrayList<>();

    public Teacher(String id, String name, String department) {
        super(id, name);
        this.department = department;
    }

    public void assignAdvisee(String studentId) {
        if (studentId != null && !studentId.isBlank() && !adviseeIds.contains(studentId)) {
            adviseeIds.add(studentId);
        }
    }

    public boolean canReview(String topicCategory, int currentLoad) {
        if (!active) {
            return false;
        }
        if ("security".equalsIgnoreCase(topicCategory) || "finance".equalsIgnoreCase(topicCategory)) {
            return currentLoad < 4;
        }
        return currentLoad < 6;
    }

    public String department() {
        return department;
    }
}
