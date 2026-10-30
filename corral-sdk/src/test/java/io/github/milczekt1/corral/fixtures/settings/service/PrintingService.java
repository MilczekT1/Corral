package io.github.milczekt1.corral.fixtures.settings.service;

/** Calls {@code println(String)}, which the settings tests' rule flags, and {@code print(String)}, which it must not. */
@SuppressWarnings("java:S106")
public class PrintingService {

    public void announce(String message) {
        System.out.println(message);
    }

    public void hint(String message) {
        System.out.print(message);
    }
}
