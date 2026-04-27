package sample.metrics;

public class Person {

    private String id;
    private String name;

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void rename(String newName) {
        this.name = newName == null ? this.name : newName;
    }

    public String displayLabel() {
        return id + "-" + name;
    }
}
