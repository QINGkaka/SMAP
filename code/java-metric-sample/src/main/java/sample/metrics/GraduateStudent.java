package sample.metrics;

public class GraduateStudent extends Student {

    private String thesisTopic;
    private int publishedPapers;
    private boolean defensePassed;

    public GraduateStudent(String id, String name, Advisor advisor) {
        super(id, name, advisor);
        this.thesisTopic = null;
        this.publishedPapers = 0;
        this.defensePassed = false;
    }

    public void updateTopic(String topic) {
        thesisTopic = topic;
    }

    public void publishPaper() {
        publishedPapers++;
    }

    public boolean canGraduate() {
        return publishedPapers >= 2 && thesisTopic != null;
    }

    public void approveDefense(boolean completedCredits) {
        if (completedCredits && canGraduate()) {
            defensePassed = true;
        } else {
            defensePassed = false;
        }
    }
}
