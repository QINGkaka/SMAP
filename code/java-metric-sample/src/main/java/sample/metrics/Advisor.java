package sample.metrics;

public class Advisor {

    private String name;
    private int adviseeCount;

    public Advisor(String name) {
        this.name = name;
        this.adviseeCount = 0;
    }

    public void notifyStudent(Student student) {
        if (student != null) {
            adviseeCount++;
        }
    }

    public void acceptStudent(Student student) {
        adviseeCount++;
        notifyStudent(student);
    }

    public int currentLoad() {
        return adviseeCount;
    }
}
