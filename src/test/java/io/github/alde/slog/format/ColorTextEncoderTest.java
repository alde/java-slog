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

class ColorTextEncoderTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logbackLogger;
    private Slog log;
    private ColorTextEncoder encoder;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();

        logbackLogger = (Logger) LoggerFactory.getLogger("test.ColorTextEncoderTest");
        logbackLogger.addAppender(appender);

        log = Slog.create("test.ColorTextEncoderTest");
        encoder = new ColorTextEncoder();
        encoder.setForceColor(false);
        encoder.start();
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
    }

    @Test
    void encodesLogrusStyleFormat() {
        log.info("hello world");
        var output = encodeFirst();

        assertThat(output).contains("INFO");
        assertThat(output).contains("hello world");
        assertThat(output).endsWith("\n");
    }

    @Test
    void usesAbbreviatedLevels() {
        log.trace("t");
        log.debug("d");
        log.info("i");
        log.warn("w");
        log.error("e");

        assertThat(encode(0)).contains("TRAC");
        assertThat(encode(1)).contains("DEBU");
        assertThat(encode(2)).contains("INFO");
        assertThat(encode(3)).contains("WARN");
        assertThat(encode(4)).contains("ERRO");
    }

    @Test
    void includesTimestampInBrackets() {
        log.info("msg");
        var output = encodeFirst();

        assertThat(output).matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z\\].*");
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

        assertThat(output).contains("failed");
        assertThat(output).contains("ctx=val");
        assertThat(output).contains("java.lang.RuntimeException: test error");
    }

    @Test
    void includesAnsiCodesWhenColorEnabled() {
        encoder.setForceColor(true);
        log.info("colored");
        var output = encodeFirst();

        assertThat(output).contains("\033[");
    }

    @Test
    void excludesAnsiCodesWhenColorDisabled() {
        encoder.setForceColor(false);
        log.info("plain");
        var output = encodeFirst();

        assertThat(output).doesNotContain("\033[");
    }

    @Test
    void coloredKeysInKeyValuePairs() {
        encoder.setForceColor(true);
        log.info("event", "orderId", "123");
        var output = encodeFirst();

        assertThat(output).contains("\033[36m" + "orderId" + "\033[0m");
    }

    @Test
    void headerAndFooterAreEmpty() {
        assertThat(encoder.headerBytes()).isEmpty();
        assertThat(encoder.footerBytes()).isEmpty();
    }

    private String encodeFirst() {
        return encode(0);
    }

    private String encode(int index) {
        return new String(encoder.encode(appender.list.get(index)), StandardCharsets.UTF_8);
    }
}
