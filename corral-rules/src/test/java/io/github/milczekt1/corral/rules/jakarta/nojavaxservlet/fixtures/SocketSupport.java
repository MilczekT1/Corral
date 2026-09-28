package io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures;

import java.io.IOException;
import java.net.Socket;
import javax.net.SocketFactory;

/** MUST IGNORE: {@code javax.net} is a JDK {@code javax} package, so a predicate widened to {@code javax..} fails. */
public class SocketSupport {

    private final SocketFactory socketFactory;

    public SocketSupport(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public Socket open(String host, int port) throws IOException {
        return socketFactory.createSocket(host, port);
    }
}
