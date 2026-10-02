package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

/**
 * MUST IGNORE: calls a {@code createStatement} that is not on a {@code Connection}, so a predicate
 * dropping the owner fails here.
 */
public class BankStatements {

    public String monthly(String account) {
        return createStatement(account, "monthly");
    }

    private String createStatement(String account, String period) {
        return period + " statement for " + account;
    }
}
