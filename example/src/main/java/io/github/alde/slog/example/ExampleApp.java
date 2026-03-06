package io.github.alde.slog.example;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.alde.slog.Slog;
import io.github.alde.slog.format.ColorTextEncoder;
import io.github.alde.slog.format.JsonEncoder;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ExampleApp {

    public record OrderCtx(String orderId, String userId, int amount) {}
    public record PaymentCtx(String paymentId, String method) {}

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--json")) {
            configureOutput(new JsonEncoder());
        } else if (args.length > 0 && args[0].equals("--color")) {
            var encoder = new ColorTextEncoder();
            encoder.setForceColor(true);
            configureOutput(encoder);
        }

        var log = Slog.create(ExampleApp.class);

        // Log with a Record
        log.info("order placed", new OrderCtx("ord-123", "bob", 4200));

        // Log with a Map
        log.info("inventory checked", Map.of("sku", "WIDGET-1", "inStock", true));

        // Log with inline key-value pairs
        log.info("shipping calculated", "orderId", "ord-123", "carrier", "FedEx", "cost", 599);

        // Child logger with default fields
        var reqLog = log.with("requestId", "req-abc", "tenant", "acme-corp");
        reqLog.info("processing request");
        reqLog.debug("validating payment", new PaymentCtx("pay-456", "credit_card"));

        // Error with exception
        var ex = new RuntimeException("insufficient funds");
        log.error("payment failed", ex, new OrderCtx("ord-123", "bob", 4200));

        // Chained with()
        var auditLog = reqLog.with("audit", true);
        auditLog.warn("rate limit approaching", "currentRate", 95, "maxRate", 100);

        log.info("example complete");
    }

    private static void configureOutput(ch.qos.logback.core.encoder.Encoder<ILoggingEvent> encoder) {
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        var root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();

        encoder.setContext(context);
        encoder.start();

        var appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.start();

        root.addAppender(appender);
    }
}
