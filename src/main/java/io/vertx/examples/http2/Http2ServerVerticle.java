package io.vertx.examples.http2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SSLEngine;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  private static final int DEFAULT_LATENCY = 100;
  private String host = "localhost"; // TODO : check host if in the cloud

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


  private HttpServerOptions createOptions(boolean http2) {
    HttpServerOptions serverOptions = new HttpServerOptions()
        .setPort(http2 ? HTTP2_PORT : HTTP1_PORT)
        .setHost(host)
        .setSsl(true);
    if (http2) {
      serverOptions
          .setSslEngine(SSLEngine.OPENSSL)
          .setPemKeyCertOptions(getPemOptions());
    } else {
      serverOptions.setKeyStoreOptions(getJksOptions());
    }
    return serverOptions;
  }

  private void createRouter() {
    router = Router.router(vertx);
    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
    engine.setMaxCacheSize(0);
    router.get("/*").handler(rc -> {
      int queryLatency = DEFAULT_LATENCY;
      try {
        queryLatency = Integer.valueOf(rc.request().getParam("latency"));
      } catch(NumberFormatException nfe) {}
      rc.put("query-latency", queryLatency);
      vertx.setTimer(queryLatency, id -> rc.next());
    });
    router.getWithRegex(".+\\.hbs").handler(ctx -> {
      final Stream<Integer> availableLatencies = Stream.of(100, 200, 300, 400);
      Integer queryLatency = ctx.get("query-latency");
      ctx.put("imgs", createImages(queryLatency));
      ctx.put("tileHeight", TILE_HEIGHT);
      ctx.put("tileWidth", TILE_WIDTH);
      ctx.put("host", host);
      ctx.put("http1Port", HTTP1_PORT);
      ctx.put("http2Port", HTTP2_PORT);
      Stream<DisplayedLatency> displayedLatencies = availableLatencies.map(latency -> new DisplayedLatency(latency, queryLatency));
      ctx.put("latencies", displayedLatencies.collect(Collectors.toList()));
      ctx.next();
    });
    router.getWithRegex(".+\\.hbs").handler(TemplateHandler.create(engine));
    router.get("/assets/*").handler(StaticHandler.create());
  }

  private List<List<String>> createImages(Integer latency) {
    List<List<String>> imgs = new ArrayList<>(COLS);
    for (int i = 0; i < COLS; i++) {
      List<String> rowImgs = new ArrayList<>(ROWS);
      for (int j = 0; j < ROWS; j++) {
        rowImgs.add("/assets/img/stairway_to_heaven-" + i + "-" + j + ".jpeg?latency=" + latency + "&cachebuster=" + cacheBuster());
      }
      imgs.add(rowImgs);
    }
    return imgs;
  }

  private String cacheBuster() {
    return Long.toString(new Date().getTime()) + RANDOM.nextLong();
  }

  private static JksOptions getJksOptions() {
    return new JksOptions().setPath("tls/server-keystore.jks").setPassword("wibble");
  }

  private static PemKeyCertOptions getPemOptions() {
    return new PemKeyCertOptions().setCertPath("tls/certificate.pem").setKeyPath("tls/pkcskey.pem");
  }
}
