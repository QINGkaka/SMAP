public class PaymentOrder {
    private final String orderId;
    private final String studentId;
    private final double amount;
    private String state;

    public PaymentOrder(String orderId, String studentId, double amount) {
        this.orderId = orderId;
        this.studentId = studentId;
        this.amount = amount;
        this.state = "CREATED";
    }

    public void markPaid() {
        state = "PAID";
    }

    public void markRejected() {
        state = "REJECTED";
    }

    public double amount() {
        return amount;
    }

    public String state() {
        return state;
    }
}
