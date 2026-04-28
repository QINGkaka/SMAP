public class GraduateStudent extends Student {
    private String thesisTitle;
    private int reviewScore;

    public GraduateStudent(String id, String name, int credits, double gpa, String thesisTitle) {
        super(id, name, credits, gpa);
        this.thesisTitle = thesisTitle;
    }

    public boolean approveDefense(int plagiarismRate, int progressScore) {
        if (plagiarismRate > 20) {
            return false;
        }
        if (progressScore >= 85 && gpa() >= 3.0) {
            reviewScore = progressScore;
            return true;
        }
        if (progressScore >= 75 && credits() >= 28 && !isAtRisk()) {
            reviewScore = progressScore - 5;
            return true;
        }
        return false;
    }

    public boolean canGraduate() {
        return credits() >= 30 && gpa() >= 2.8 && reviewScore >= 80;
    }

    public String thesisTitle() {
        return thesisTitle;
    }
}
