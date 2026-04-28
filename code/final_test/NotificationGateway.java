public interface NotificationGateway {
    boolean send(String target, String message, boolean urgent);
}
