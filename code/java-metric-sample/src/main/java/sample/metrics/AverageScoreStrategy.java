package sample.metrics;

public class AverageScoreStrategy implements ScoreStrategy {

    @Override
    public int normalize(int score) {
        if (score < 0) {
            return 0;
        }
        if (score > 100) {
            return 100;
        }
        return score;
    }
}
