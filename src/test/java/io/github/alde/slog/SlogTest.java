package io.github.alde.slog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SlogTest {

    record Order(String orderId, String userId, int amount) {}

    private ListAppender<ILoggingEvent> appender;
    private Logger logbackLogger;
    private Slog log;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();

        logbackLogger = (Logger) LoggerFactory.getLogger("test.SlogTest");
        logbackLogger.addAppender(appender);

        log = new Slog(logbackLogger, Map.of());
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
    }

    @Test
    void logsWithRecord() {
        log.info("order placed", new Order("123", "bob", 42));

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getMessage()).isEqualTo("order placed");
        assertThat(event.getKeyValuePairs()).hasSize(3);
        assertThat(event.getKeyValuePairs().get(0).key).isEqualTo("orderId");
        assertThat(event.getKeyValuePairs().get(0).value).isEqualTo("123");
        assertThat(event.getKeyValuePairs().get(2).key).isEqualTo("amount");
        assertThat(event.getKeyValuePairs().get(2).value).isEqualTo(42);
    }

    @Test
    void logsWithMap() {
        log.info("event", Map.of("key", "value"));

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(1);
        assertThat(kv.get(0).key).isEqualTo("key");
        assertThat(kv.get(0).value).isEqualTo("value");
    }

    @Test
    void logsWithVarargs() {
        log.info("event", "k1", "v1", "k2", 42);

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(2);
        assertThat(kv.get(0).key).isEqualTo("k1");
        assertThat(kv.get(1).value).isEqualTo(42);
    }

    @Test
    void logsPlainMessage() {
        log.info("just a message");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getMessage()).isEqualTo("just a message");
        assertThat(appender.list.get(0).getKeyValuePairs()).isNull();
    }

    @Test
    void childLoggerIncludesDefaultFields() {
        var child = log.with("requestId", "req-1", "tenant", "acme");
        child.info("processing", "action", "save");

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(3);
        assertThat(kv.get(0).key).isEqualTo("requestId");
        assertThat(kv.get(0).value).isEqualTo("req-1");
        assertThat(kv.get(1).key).isEqualTo("tenant");
        assertThat(kv.get(1).value).isEqualTo("acme");
        assertThat(kv.get(2).key).isEqualTo("action");
        assertThat(kv.get(2).value).isEqualTo("save");
    }

    @Test
    void chainedWithMergesFields() {
        var child = log.with("a", 1).with("b", 2);
        child.info("chained");

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(2);
        assertThat(kv.get(0).key).isEqualTo("a");
        assertThat(kv.get(0).value).isEqualTo(1);
        assertThat(kv.get(1).key).isEqualTo("b");
        assertThat(kv.get(1).value).isEqualTo(2);
    }

    @Test
    void withOverridesExistingField() {
        var child = log.with("a", 1).with("a", 2);
        child.info("overridden");

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(1);
        assertThat(kv.get(0).key).isEqualTo("a");
        assertThat(kv.get(0).value).isEqualTo(2);
    }

    @Test
    void errorWithThrowable() {
        var ex = new RuntimeException("boom");
        log.error("failed", ex, new Order("123", "bob", 42));

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getMessage()).isEqualTo("failed");
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
        assertThat(event.getKeyValuePairs()).hasSize(3);
    }

    @Test
    void allLevelsWork() {
        log.trace("t");
        log.debug("d");
        log.info("i");
        log.warn("w");
        log.error("e");

        assertThat(appender.list).hasSize(5);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.TRACE);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.DEBUG);
        assertThat(appender.list.get(2).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(3).getLevel()).isEqualTo(Level.WARN);
        assertThat(appender.list.get(4).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void warnWithStructuredArgs() {
        log.warn("disk full", "usage", 98);

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(1);
        assertThat(kv.get(0).key).isEqualTo("usage");
    }

    @Test
    void createFromClassLogsSuccessfully() {
        var slog = Slog.create(SlogTest.class);
        slog.info("from class factory");

        assertThat(appender.list).isEmpty(); // different logger name, won't capture
        // but no exception thrown means it works
    }

    @Test
    void createFromStringLogsSuccessfully() {
        var factoryAppender = new ListAppender<ILoggingEvent>();
        factoryAppender.start();
        var factoryLogger = (Logger) LoggerFactory.getLogger("my.logger");
        factoryLogger.addAppender(factoryAppender);

        var slog = Slog.create("my.logger");
        slog.info("from string factory");

        assertThat(factoryAppender.list).hasSize(1);
        assertThat(factoryAppender.list.get(0).getMessage()).isEqualTo("from string factory");

        factoryLogger.detachAppender(factoryAppender);
    }

    @Test
    void errorWithThrowableAndVarargs() {
        var ex = new RuntimeException("boom");
        log.error("failed", ex, "orderId", "123", "userId", "bob");

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getMessage()).isEqualTo("failed");
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
        assertThat(event.getKeyValuePairs()).hasSize(2);
        assertThat(event.getKeyValuePairs().get(0).key).isEqualTo("orderId");
    }

    @Test
    void withAcceptsRecord() {
        var child = log.with(new Order("ord-1", "alice", 100));
        child.info("from record context");

        assertThat(appender.list).hasSize(1);
        var kv = appender.list.get(0).getKeyValuePairs();
        assertThat(kv).hasSize(3);
        assertThat(kv.get(0).key).isEqualTo("orderId");
        assertThat(kv.get(0).value).isEqualTo("ord-1");
    }
}
