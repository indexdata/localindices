/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.reshare;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import static io.vertx.core.impl.VertxImpl.context;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.UUID;

/**
 *
 * @author kurt
 */
public class MainVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(MainVerticle.class.getClass().getName());

  public void start(Future<Void> future) {
    final String defaultPort = context.config().getString("port", "9130");
    final String portStr = System.getProperty("port", defaultPort);
    final int port = Integer.parseInt(portStr);

    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer();

    router.route("/*").handler(BodyHandler.create());
    router.route("/instances/:id").handler(this::handleInstances);
    router.route("/instances").handler(this::handleInstances);
    router.route("/bl-users/login").handler(this::handleLogin);

    logger.info("Running MIS-Mock on port " + port);
    server.requestHandler(router::accept).listen(port, result -> {
      if(result.failed()) {
        future.fail(result.cause());
      } else {
        future.complete();
      }
    });

  }

  private void handleInstances(RoutingContext context) {
    String id = context.request().getParam("id");
    if(context.request().method() == HttpMethod.POST) {
      String content = context.getBodyAsString();
      logger.info(String.format("Got POST request %s, payload %s", context.request().absoluteURI(), content));
      context.response().setStatusCode(201)
          .end(content);
    } else if (context.request().method() == HttpMethod.GET) {
      logger.info(String.format("Got GET request for id '%s' %s", id, context.request().absoluteURI()));
      context.response().setStatusCode(200)
          .end(new JsonObject().put("content", context.request().absoluteURI()).encode());
    } else if (context.request().method() == HttpMethod.DELETE) {
      logger.info(String.format("Got DELETE request for id '%s' %s", id, context.request().absoluteURI()));
      context.response().setStatusCode(204)
          .end();
    } else {
      String message = String.format("Unsupported method %s", context.request().method().toString());
      context.response().setStatusCode(400)
          .end(message);
    }
  }

  private void handleLogin(RoutingContext context) {
    if(context.request().method() != HttpMethod.POST) {
      String message = String.format("Unsupported method %s", context.request().method().toString());
      context.response().setStatusCode(400)
          .end(message);
    } else {
      String content = context.getBodyAsString();
      String fakeToken = UUID.randomUUID().toString();
      logger.info(String.format("Got POST to login %s, payload %s", context.request().absoluteURI(), content));
      context.response().setStatusCode(201)
          .putHeader("X-Okapi-Token", fakeToken)
          .end(content);
    }
  }
}
