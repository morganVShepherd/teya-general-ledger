package moo.interview.teya.controller.constants;

public final class ApiPaths {

    private ApiPaths() {
    }

    public static final String V1 = "/v1";
    public static final String ACCOUNTS = V1 + "/accounts";
    public static final String ACCOUNT_BALANCE = ACCOUNTS + "/{accountNumber}/balance";
    public static final String ACCOUNT_TRANSACTIONS = ACCOUNTS + "/{accountNumber}/transactions";
}

