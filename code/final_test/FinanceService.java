import java.util.List;

public class FinanceService {
    private final NotificationGateway notificationGateway;

    public FinanceService(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    public double settle(List<PaymentOrder> orders, boolean strictMode) {
        double total = 0;
        for (PaymentOrder order : orders) {
            if (order == null || order.amount() <= 0) {
                continue;
            }
            if (strictMode && order.amount() > 5000) {
                order.markRejected();
                continue;
            }
            total += order.amount();
            order.markPaid();
        }
        return total;
    }

    public String classify(PaymentOrder order) {
        if (order == null) {
            return "INVALID";
        }
        if (order.amount() >= 10000) {
            return "LARGE";
        }
        if (order.amount() >= 3000) {
            return "MEDIUM";
        }
        return "SMALL";
    }

    public boolean remind(String target, int overdueDays) {
        if (overdueDays <= 0) {
            return false;
        }
        return notificationGateway.send(target, "payment overdue: " + overdueDays, overdueDays >= 5);
    }
}
