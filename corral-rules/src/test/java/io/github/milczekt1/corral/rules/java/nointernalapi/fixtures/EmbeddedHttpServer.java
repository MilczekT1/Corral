package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/** MUST IGNORE: {@code com.sun.net.httpserver} is supported, exported JDK API. */
public class EmbeddedHttpServer {

    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.start();
        return server;
    }
}
