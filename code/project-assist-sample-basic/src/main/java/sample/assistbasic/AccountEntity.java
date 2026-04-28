package sample.assistbasic;

public class AccountEntity {
    private String accountId;
    private String ownerName;
    private String region;
    private boolean archived;

    public String accountId() {
        return accountId;
    }

    public String ownerName() {
        return ownerName;
    }

    public String region() {
        return region;
    }

    public boolean archived() {
        return archived;
    }
}
