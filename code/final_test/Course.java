public class Course {
    private final String code;
    private final String name;
    private final int credits;
    private int capacity;
    private boolean waitListEnabled;

    public Course(String code, String name, int credits, int capacity, boolean waitListEnabled) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.capacity = capacity;
        this.waitListEnabled = waitListEnabled;
    }

    public boolean canEnroll(int currentSize, boolean hasConflict, boolean accountFrozen) {
        if (accountFrozen || hasConflict) {
            return false;
        }
        return currentSize < capacity || waitListEnabled;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int credits() {
        return credits;
    }
}
