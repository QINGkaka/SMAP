package sample.metrics;

public class SplitStorage {

    private int openCount;
    private int closeCount;

    public int readOpen() {
        return openCount;
    }

    public void resetOpen() {
        openCount = 0;
    }

    public int readClose() {
        return closeCount;
    }

    public void resetClose() {
        closeCount = 0;
    }
}
