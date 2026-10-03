package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import jdk.jfr.Event;

/** MUST IGNORE: {@code jdk.jfr} is supported JDK API outside {@code jdk.internal}. */
public class CheckoutEvent extends Event {

    public long orderId;
}
