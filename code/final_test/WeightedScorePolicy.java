import java.util.List;

public class WeightedScorePolicy implements ScorePolicy {
    @Override
    public double calculate(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0;
        }
        double total = 0;
        int index = 1;
        for (Integer score : scores) {
            if (score == null) {
                index++;
                continue;
            }
            int weight = index == 1 ? 3 : index == 2 ? 2 : 1;
            total += score * weight;
            index++;
        }
        return total / (scores.size() + 2.0);
    }
}
