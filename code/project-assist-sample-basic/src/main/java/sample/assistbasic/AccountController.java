package sample.assistbasic;

import java.util.List;

public class AccountController {
    public String createAccount(AccountEntity account) {
        return account.accountId();
    }

    public AccountEntity getAccountDetail(String accountId) {
        return null;
    }

    public List<AccountEntity> listAccounts() {
        return List.of();
    }

    public String exportAccountSummary(String region, int year, boolean archived) {
        return region + year + archived;
    }
}
