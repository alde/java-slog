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

class KeyValueEncoderTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logbackLogger;
    private Slog log;
    private KeyValueEncoder encoder;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();

        logbackLogger = (Logger) LoggerFactory.getLogger("test.KVEncoderTest");
        logbackLogger.addAppender(appender);

        log = Slog.create("test.KVEncoderTest");
        encoder = new KeyValueEncoder();
        encoder.start();
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
    }

    @Test
    void encodesBasicFormat() {
        log.info("hello world");
        var output = encodeFirst();

        assertThat(output).contains("INFO ");
        assertThat(output).contains("test.KVEncoderTest");
        assertThat(output).contains(" - hello world");
        assertThat(output).endsWith("\n");
    }

    @Test
    void encodesKeyValuePairs() {
        log.info("event", "orderId", "123", "amount", 42);
        var output = encodeFirst();

        assertThat(output).contains("orderId=123");
        assertThat(output).contains("amount=42");
    }

    @Test
    void quotesValuesWithSpaces() {
        log.info("event", "name", "John Doe");
        var output = encodeFirst();

        assertThat(output).contains("name=\"John Doe\"");
    }

    @Test
    void quotesEmptyValues() {
        log.info("event", "empty", "");
        var output = encodeFirst();

        assertThat(output).contains("empty=\"\"");
    }

    @Test
    void handlesNullValues() {
        log.info("event", "missing", null);
        var output = encodeFirst();

        assertThat(output).contains("missing=null");
    }

    @Test
    void encodesThrowable() {
        var ex = new RuntimeException("test error");
        log.error("failed", ex, Map.of("ctx", "val"));
        var output = encodeFirst();

        assertThat(output).contains("- failed");
        assertThat(output).contains("ctx=val");
        assertThat(output).contains("java.lang.RuntimeException: test error");
    }

    @Test
    void escapesNewlinesInValues() {
        log.info("event", "desc", "line1\nline2");
        var output = encodeFirst();

        assertThat(output).contains("desc=\"line1\\nline2\"");
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
