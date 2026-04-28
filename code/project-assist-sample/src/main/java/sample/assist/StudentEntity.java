package sample.assist;

public class StudentEntity {
    private String studentId;
    private String name;
    private String major;
    private int credits;
    private boolean warning;

    public String studentId() {
        return studentId;
    }

    public String name() {
        return name;
    }

    public String major() {
        return major;
    }

    public int credits() {
        return credits;
    }

    public boolean warning() {
        return warning;
    }
}
