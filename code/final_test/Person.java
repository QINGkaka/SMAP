public class Person {
    protected final String id;
    protected String name;
    protected boolean active;

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean active() {
        return active;
    }

    public void rename(String nextName) {
        this.name = nextName == null || nextName.isBlank() ? this.name : nextName.trim();
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
