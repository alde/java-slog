package io.github.alde.slog.format;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.alde.slog.Slog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logbackLogger;
    private Slog log;
    private JsonEncoder encoder;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();

        logbackLogger = (Logger) LoggerFactory.getLogger("test.JsonEncoderTest");
        logbackLogger.addAppender(appender);

        log = Slog.create("test.JsonEncoderTest");
        encoder = new JsonEncoder();
        encoder.start();
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
    }

    @Test
    void encodesBasicFields() {
        log.info("hello");
        var json = encodeFirst();

        assertThat(json).contains("\"level\":\"INFO\"");
        assertThat(json).contains("\"logger\":\"test.JsonEncoderTest\"");
        assertThat(json).contains("\"message\":\"hello\"");
        assertThat(json).contains("\"timestamp\":");
        assertThat(json).endsWith("}\n");
    }

    @Test
    void encodesKeyValuePairs() {
        log.info("event", "orderId", "123", "amount", 42);
        var json = encodeFirst();

        assertThat(json).contains("\"orderId\":\"123\"");
        assertThat(json).contains("\"amount\":42");
    }

    @Test
    void encodesNumbersWithoutQuotes() {
        log.info("event", "count", 7);
        var json = encodeFirst();

        assertThat(json).contains("\"count\":7");
    }

    @Test
    void encodesBooleansWithoutQuotes() {
        log.info("event", "active", true);
        var json = encodeFirst();

        assertThat(json).contains("\"active\":true");
    }

    @Test
    void encodesNullValues() {
        log.info("event", "missing", null);
        var json = encodeFirst();

        assertThat(json).contains("\"missing\":null");
    }

    @Test
    void escapesSpecialCharacters() {
        log.info("msg with \"quotes\" and\nnewline");
        var json = encodeFirst();

        assertThat(json).contains("\\\"quotes\\\"");
        assertThat(json).contains("\\n");
    }

    @Test
    void encodesThrowable() {
        var ex = new RuntimeException("test error");
        log.error("failed", ex, Map.of("ctx", "val"));
        var json = encodeFirst();

        assertThat(json).contains("\"error\":\"java.lang.RuntimeException: test error\"");
        assertThat(json).contains("\"stacktrace\":");
    }

    @Test
    void headerAndFooterAreEmpty() {
        assertThat(encoder.headerBytes()).isEmpty();
        assertThat(encoder.footerBytes()).isEmpty();
    }

    private String encodeFirst() {
        return new String(encoder.encode(appender.list.get(0)), StandardCharsets.UTF_8);
    }
}
