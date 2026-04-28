import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentRepository {
    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new HashMap<>();
    private final List<EnrollmentRecord> enrollments = new ArrayList<>();

    public void saveStudent(Student student) {
        students.put(student.id(), student);
    }

    public void saveCourse(Course course) {
        courses.put(course.code(), course);
    }

    public Student findStudent(String studentId) {
        return students.get(studentId);
    }

    public Course findCourse(String courseCode) {
        return courses.get(courseCode);
    }

    public void addEnrollment(EnrollmentRecord record) {
        enrollments.add(record);
    }

    public List<EnrollmentRecord> findEnrollmentsByStudent(String studentId) {
        List<EnrollmentRecord> result = new ArrayList<>();
        for (EnrollmentRecord record : enrollments) {
            if (record != null && studentId.equals(record.studentId())) {
                result.add(record);
            }
        }
        return result;
    }
}
