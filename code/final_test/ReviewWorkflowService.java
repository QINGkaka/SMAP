import java.util.List;

public class ReviewWorkflowService {
    public boolean approve(String category, int score, boolean conflict, boolean plagiarism) {
        if (plagiarism || conflict) {
            return false;
        }
        if ("thesis".equalsIgnoreCase(category)) {
            return score >= 85;
        }
        if ("finance".equalsIgnoreCase(category)) {
            return score >= 80;
        }
        return score >= 75;
    }

    public int assignLoad(List<Teacher> reviewers, String topicCategory) {
        int available = 0;
        for (Teacher reviewer : reviewers) {
            if (reviewer != null && reviewer.canReview(topicCategory, available)) {
                available++;
            }
        }
        return available;
    }

    public String summarize(int approved, int rejected, int pending) {
        if (pending > approved && pending > rejected) {
            return "BACKLOG";
        }
        if (rejected > approved) {
            return "BLOCKED";
        }
        return approved >= 5 ? "STABLE" : "ACTIVE";
    }
}
