package io.vertx.examples.http2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Http2ServerVerticle extends AbstractVerticle {

    private static final int HTTP1_PORT = 4043;
    private static final int HTTP2_PORT = 4044;

    private static final int COLS = 15;
    private static final int ROWS = 15;
    private static final int TILE_HEIGHT = 38;
    private static final int TILE_WIDTH = 68;
    private static final Random RANDOM = new Random();

    private HttpServer http1;
    private HttpServer http2;
    private Router router;


    @Override
    public void start(Future<Void> future) {
        createRouter();
        http1 = vertx.createHttpServer(createOptions(false));
        http1.requestHandler(router::accept);
        http1.listen(res -> {
            if (res.failed()) {
                future.fail(res.cause());
                return;
            }
            http2 = vertx.createHttpServer(createOptions(true));
            http2.requestHandler(router::accept);
            http2.listen(res2 -> {
                if (res2.failed()) {
                    future.fail(res.cause());
                } else {
                    future.complete();
                }
            });
        });
    }

    @Override
    public void stop(Future<Void> future) {
        http1.close(res -> http2.close(future.completer()));
    }


    private static HttpServerOptions createOptions(boolean http2) {
        HttpServerOptions serverOptions = new HttpServerOptions()
            .setPort(http2 ? HTTP2_PORT : HTTP1_PORT)
            .setHost("localhost")
            .setSsl(true)
            .setKeyStoreOptions(getJksOptions());
        if (http2) {
            serverOptions.setUseAlpn(true);
        }
        return serverOptions;
    }

    private void createRouter() {
        router = Router.router(vertx);
        HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
        engine.setMaxCacheSize(0);
        router.getWithRegex(".+\\.hbs").handler(ctx -> {
            List<List<String>> imgs = new ArrayList<>(COLS);
            for (int i = 0; i < COLS; i++) {
                List<String> rowImgs = new ArrayList<>(ROWS);
                for (int j = 0; j < ROWS; j++) {
                    rowImgs.add("/assets/img/stairway_to_heaven-" + i + "-" + j + ".jpeg?cachebuster=" + cacheBuster());
                }
                imgs.add(rowImgs);
            }
            ctx.put("imgs", imgs);
            ctx.put("tileHeight", TILE_HEIGHT);
            ctx.put("tileWidth", TILE_WIDTH);
            ctx.next();
        });
        router.getWithRegex(".+\\.hbs").handler(TemplateHandler.create(engine));
        router.get("/assets/*").handler(rc -> {
          vertx.setTimer(140, id -> rc.next());
        });
        router.get("/assets/*").handler(StaticHandler.create());
    }

    private String cacheBuster() {
        return Long.toString(new Date().getTime()) + RANDOM.nextLong();
    }

    private static JksOptions getJksOptions() {
        return new JksOptions().setPath("tls/server-keystore.jks").setPassword("wibble");
    }

}
