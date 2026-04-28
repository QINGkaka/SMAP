package sample.assist;

import java.util.List;

public class CourseController {
    public String createCourse(CourseEntity course) {
        return course.courseId();
    }

    public boolean updateCourse(String courseId, CourseEntity course) {
        return courseId != null && course != null;
    }

    public List<CourseEntity> listCourses() {
        return List.of();
    }
}
