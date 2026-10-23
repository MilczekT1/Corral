package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.v2;

/** MUST IGNORE: digits are not uppercase letters. */
public class Receipt {

    public String number(int year, int sequence) {
        return year + "-" + sequence;
    }
}
