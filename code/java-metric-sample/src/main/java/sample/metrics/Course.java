package sample.metrics;

public class Course {

    private String code;
    private String title;
    private int capacity;

    public Course(String code, String title, int capacity) {
        this.code = code;
        this.title = title;
        this.capacity = capacity;
    }

    public boolean isFull(int currentCount) {
        return currentCount >= capacity;
    }

    public String formatLabel() {
        return code + ":" + title;
    }

    public boolean hasCode(String candidate) {
        return candidate != null && candidate.equals(code);
    }
}
