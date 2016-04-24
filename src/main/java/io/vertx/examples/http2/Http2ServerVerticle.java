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

  private static final int COLS = 15;
  private static final int ROWS = 15;
  private static final int TILE_HEIGHT = 38;
  private static final int TILE_WIDTH = 68;
  private static final Random RANDOM = new Random();

  private HttpServer http;
  private Router router;

  private static HttpServerOptions createOptions() {
    HttpServerOptions serverOptions = new HttpServerOptions()
        .setPort(getPort())
        .setHost(getHost())
        .setSsl(true)
        .setKeyStoreOptions(getJksOptions());
    if (isHttp2()) {
      serverOptions.setUseAlpn(true);
    }
    return serverOptions;
  }

  private static JksOptions getJksOptions() {
    return new JksOptions().setPath("tls/server-keystore.jks").setPassword("wibble");
  }

  private static int getPort() {
    return Integer.valueOf(System.getenv("OPENSHIFT_DIY_PORT"));
  }

  private static String getHost() {
    return System.getenv("OPENSHIFT_DIY_IP");
  }

  private static boolean isHttp2() {
    return System.getProperty("http2") != null;
  }

  @Override
  public void start(Future<Void> future) {
    createRouter();
    http = vertx.createHttpServer(createOptions());
    http.requestHandler(router::accept);
    http.listen(res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }
      future.complete();
    });
  }

  @Override
  public void stop(Future<Void> future) {
    http.close(future.completer());
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
    router.get("/assets/*").handler(StaticHandler.create());
  }

  private String cacheBuster() {
    return Long.toString(new Date().getTime()) + RANDOM.nextLong();
  }

}
