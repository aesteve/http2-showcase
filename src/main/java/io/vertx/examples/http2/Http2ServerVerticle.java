package io.vertx.examples.http2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import static io.vertx.core.http.HttpHeaders.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Http2ServerVerticle extends AbstractVerticle {


    private HttpServer server;

    private final static int cols = 15;
    private final static int rows = 15;
    private final static int TILE_HEIGHT = 38;
    private final static int TILE_WIDTH = 68;

    @Override
    public void start(Future<Void> future) {
        server = vertx.createHttpServer(createOptions());
        Router router = Router.router(vertx);
        HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
        engine.setMaxCacheSize(0);
        router.getWithRegex(".+\\.hbs").handler(ctx -> {
            List<List<String>> imgs = new ArrayList<>(cols);
            for (int i = 0; i < cols; i++) {
                List<String> rowImgs = new ArrayList<>(rows);
                for (int j = 0; j < rows; j++) {
                    rowImgs.add("https://localhost:4043/assets/img/stairway_to_heaven-" + i + "-" + j + ".jpeg?cachebuster=" + new Date().getTime());
                }
                imgs.add(rowImgs);
            }
            ctx.put("imgs", imgs);
            ctx.put("tileHeight", TILE_HEIGHT);
            ctx.put("tileWidth", TILE_WIDTH);
            ctx.next();
        });
        router.getWithRegex(".+\\.hbs").handler(TemplateHandler.create(engine));
        router.get("/assets/*").handler(ctx -> {
           MultiMap headers = ctx.response().headers();
            headers.add(EXPIRES, "0");
            headers.add(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            ctx.next();
        });
        router.get("/assets/*").handler(StaticHandler.create().setCachingEnabled(false));
        server.requestHandler(router::accept);
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
                //.setUseAlpn(true)
                .setSsl(true)
                .setKeyStoreOptions(getJksOptions());
        return serverOptions;

    }

    private static JksOptions getJksOptions() {
        return new JksOptions().setPath("tls/server-keystore.jks").setPassword("wibble");
    }

}
