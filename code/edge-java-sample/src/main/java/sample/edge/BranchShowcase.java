package sample.edge;

public class BranchShowcase {
    public String classify(int score, boolean urgent) {
        if (score >= 90 && urgent) {
            return "A";
        }
        if (score >= 75 || urgent) {
            return "B";
        }
        return score < 60 ? "C" : "D";
    }

    public int resolveRisk(int level) {
        return switch (level) {
            case 5 -> 3;
            case 4 -> 2;
            case 3 -> 1;
            default -> 0;
        };
    }
}
