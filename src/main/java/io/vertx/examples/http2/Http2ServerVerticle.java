package io.vertx.examples.http2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class Http2ServerVerticle extends AbstractVerticle {


    private HttpServer server;

    @Override
    public void start(Future<Void> future) {
        server = vertx.createHttpServer(createOptions());
        server.requestHandler(req -> req.response().end("Hello world"));
        server.listen(res -> {
           if (res.failed()) {
               future.fail(res.cause());
           } else {
               future.complete();
           }
        });
    }

    @Override
    public void stop(Future<Void> future) {
        if (server == null) {
            future.complete();
            return;
        }
        server.close(future.completer());
    }


    private static HttpServerOptions createOptions() {
        HttpServerOptions serverOptions = new HttpServerOptions()
                .setPort(4043)
                .setHost("localhost")
                .setUseAlpn(true)
                .setSsl(true)
                .setKeyStoreOptions(getJksOptions());
        return serverOptions;

    }

    private static JksOptions getJksOptions() {
        return new JksOptions().setPath("tls/server-keystore.jks").setPassword("wibble");
    }

}
