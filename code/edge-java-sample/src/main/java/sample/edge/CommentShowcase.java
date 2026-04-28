package sample.edge;

public class CommentShowcase {
    // single-line comment
    private int threshold;

    /*
     * multi-line comment
     * used for line counting
     */
    public CommentShowcase(int threshold) {
        this.threshold = threshold;
    }

    public int sumPositive(int[] values) {
        int sum = 0;

        for (int value : values) {
            if (value > 0) {
                sum += value;
            }
        }

        return sum;
    }
}
