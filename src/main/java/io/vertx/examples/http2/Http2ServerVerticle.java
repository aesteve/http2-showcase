package io.vertx.examples.http2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.*;
import io.vertx.core.net.JksOptions;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Http2ServerVerticle extends AbstractVerticle {

    public final static int HTTP1_PORT = 4043;
    public final static int HTTP2_PORT = 4044;

    private final static int cols = 15;
    private final static int rows = 15;
    private final static int TILE_HEIGHT = 38;
    private final static int TILE_WIDTH = 68;

    private HttpServer http1;
    private HttpServer http2;


    @Override
    public void start(Future<Void> future) {
        http1 = vertx.createHttpServer(createOptions(false));
        http1.requestHandler(createRouter(false)::accept);
        http1.listen(res -> {
           if (res.failed()) {
               future.fail(res.cause());
           } else {
               http2 = vertx.createHttpServer(createOptions(true));
               http2.requestHandler(createRouter(true)::accept);
               http2.listen(res2 -> {
                  if (res2.failed()) {
                      future.fail(res.cause());
                  } else {
                      future.complete();
                  }
               });
           }
        });
    }

    @Override
    public void stop(Future<Void> future) {
        if (http1 == null) {
            future.complete();
            return;
        }
        http1.close(future.completer());
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

    private Router createRouter(boolean http2) {
        Router router = Router.router(vertx);
        HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
        engine.setMaxCacheSize(0);
        router.getWithRegex(".+\\.hbs").handler(ctx -> {
            List<List<String>> imgs = new ArrayList<>(cols);
            for (int i = 0; i < cols; i++) {
                List<String> rowImgs = new ArrayList<>(rows);
                for (int j = 0; j < rows; j++) {
                    rowImgs.add("/assets/img/stairway_to_heaven-" + i + "-" + j + ".jpeg?cachebuster=" + new Date().getTime());
                }
                imgs.add(rowImgs);
            }
            ctx.put("imgs", imgs);
            ctx.put("tileHeight", TILE_HEIGHT);
            ctx.put("tileWidth", TILE_WIDTH);
            ctx.next();
        });
        router.getWithRegex(".+\\.hbs").handler(TemplateHandler.create(engine));
        router.get("/assets/*").handler(StaticHandler.create());
        return router;
    }

    private static JksOptions getJksOptions() {
        return new JksOptions().setPath("tls/server-keystore.jks").setPassword("wibble");
    }

}
