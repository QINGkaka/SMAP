package sample.assist;

import java.util.List;

public class StudentController {
    public String createStudent(StudentEntity student) {
        return student.studentId();
    }

    public List<StudentEntity> listStudents() {
        return List.of();
    }

    public StudentEntity getStudentDetail(String studentId) {
        return null;
    }

    public boolean assignAdvisor(String studentId, String advisorId) {
        return studentId != null && advisorId != null;
    }

    public String exportRiskReport() {
        return "ok";
    }

    public List<CourseEntity> searchCourse(String keyword, String major) {
        return List.of();
    }
}
