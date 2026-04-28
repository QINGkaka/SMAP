public class EmailNotificationGateway implements NotificationGateway {
    @Override
    public boolean send(String target, String message, boolean urgent) {
        if (target == null || target.isBlank() || message == null || message.isBlank()) {
            return false;
        }
        return urgent || target.contains("@");
    }
}
