package io.github.alde.slog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured logger that wraps SLF4J with a clean API for key-value logging.
 * Immutable and thread-safe. Instances created via {@link #create} or {@link #with}.
 *
 * <pre>{@code
 * var log = Slog.create(MyClass.class);
 * log.info("order placed", new OrderCtx("123", "bob", 42));
 * log.info("order placed", Map.of("orderId", "123"));
 * log.info("order placed", "orderId", "123", "userId", "bob");
 * }</pre>
 */
public final class Slog {

    private final Logger logger;
    private final Map<String, Object> defaultFields;

    Slog(Logger logger, Map<String, Object> defaultFields) {
        this.logger = logger;
        this.defaultFields = Collections.unmodifiableMap(new LinkedHashMap<>(defaultFields));
    }

    /**
     * Creates a Slog instance for the given class.
     *
     * @param clazz the class to create the logger for
     * @return a new Slog instance
     */
    public static Slog create(Class<?> clazz) {
        return new Slog(LoggerFactory.getLogger(clazz), Map.of());
    }

    /**
     * Creates a Slog instance with the given logger name.
     *
     * @param name the logger name
     * @return a new Slog instance
     */
    public static Slog create(String name) {
        return new Slog(LoggerFactory.getLogger(name), Map.of());
    }

    /**
     * Logs a TRACE message.
     *
     * @param msg the log message
     */
    public void trace(String msg) {
        log(logger.atTrace(), msg, List.of());
    }

    /**
     * Logs a TRACE message with structured context from a Record or Map.
     *
     * @param msg the log message
     * @param context a Record or Map whose fields become key-value pairs
     */
    public void trace(String msg, Object context) {
        log(logger.atTrace(), msg, FieldExtractor.extract(context));
    }

    /**
     * Logs a TRACE message with structured key-value pairs.
     *
     * @param msg the log message
     * @param kvPairs alternating key-value pairs
     */
    public void trace(String msg, Object... kvPairs) {
        log(logger.atTrace(), msg, FieldExtractor.extractFromVarargs(kvPairs));
    }

    /**
     * Logs a DEBUG message.
     *
     * @param msg the log message
     */
    public void debug(String msg) {
        log(logger.atDebug(), msg, List.of());
    }

    /**
     * Logs a DEBUG message with structured context from a Record or Map.
     *
     * @param msg the log message
     * @param context a Record or Map whose fields become key-value pairs
     */
    public void debug(String msg, Object context) {
        log(logger.atDebug(), msg, FieldExtractor.extract(context));
    }

    /**
     * Logs a DEBUG message with structured key-value pairs.
     *
     * @param msg the log message
     * @param kvPairs alternating key-value pairs
     */
    public void debug(String msg, Object... kvPairs) {
        log(logger.atDebug(), msg, FieldExtractor.extractFromVarargs(kvPairs));
    }

    /**
     * Logs an INFO message.
     *
     * @param msg the log message
     */
    public void info(String msg) {
        log(logger.atInfo(), msg, List.of());
    }

    /**
     * Logs an INFO message with structured context from a Record or Map.
     *
     * @param msg the log message
     * @param context a Record or Map whose fields become key-value pairs
     */
    public void info(String msg, Object context) {
        log(logger.atInfo(), msg, FieldExtractor.extract(context));
    }

    /**
     * Logs an INFO message with structured key-value pairs.
     *
     * @param msg the log message
     * @param kvPairs alternating key-value pairs
     */
    public void info(String msg, Object... kvPairs) {
        log(logger.atInfo(), msg, FieldExtractor.extractFromVarargs(kvPairs));
    }

    /**
     * Logs a WARN message.
     *
     * @param msg the log message
     */
    public void warn(String msg) {
        log(logger.atWarn(), msg, List.of());
    }

    /**
     * Logs a WARN message with structured context from a Record or Map.
     *
     * @param msg the log message
     * @param context a Record or Map whose fields become key-value pairs
     */
    public void warn(String msg, Object context) {
        log(logger.atWarn(), msg, FieldExtractor.extract(context));
    }

    /**
     * Logs a WARN message with structured key-value pairs.
     *
     * @param msg the log message
     * @param kvPairs alternating key-value pairs
     */
    public void warn(String msg, Object... kvPairs) {
        log(logger.atWarn(), msg, FieldExtractor.extractFromVarargs(kvPairs));
    }

    /**
     * Logs an ERROR message.
     *
     * @param msg the log message
     */
    public void error(String msg) {
        log(logger.atError(), msg, List.of());
    }

    /**
     * Logs an ERROR message with structured context from a Record or Map.
     *
     * @param msg the log message
     * @param context a Record or Map whose fields become key-value pairs
     */
    public void error(String msg, Object context) {
        log(logger.atError(), msg, FieldExtractor.extract(context));
    }

    /**
     * Logs an ERROR message with a throwable and structured context from a Record or Map.
     *
     * @param msg the log message
     * @param throwable the exception to log
     * @param context a Record or Map whose fields become key-value pairs
     */
    public void error(String msg, Throwable throwable, Object context) {
        log(logger.atError().setCause(throwable), msg, FieldExtractor.extract(context));
    }

    /**
     * Logs an ERROR message with a throwable and structured key-value pairs.
     *
     * @param msg the log message
     * @param throwable the exception to log
     * @param kvPairs alternating key-value pairs
     */
    public void error(String msg, Throwable throwable, Object... kvPairs) {
        log(logger.atError().setCause(throwable), msg, FieldExtractor.extractFromVarargs(kvPairs));
    }

    /**
     * Logs an ERROR message with structured key-value pairs.
     *
     * @param msg the log message
     * @param kvPairs alternating key-value pairs
     */
    public void error(String msg, Object... kvPairs) {
        log(logger.atError(), msg, FieldExtractor.extractFromVarargs(kvPairs));
    }

    /**
     * Creates a child logger with default fields from a Record or Map.
     *
     * @param context a Record or Map whose fields become default key-value pairs
     * @return a new Slog instance with the merged fields
     */
    public Slog with(Object context) {
        return withFields(FieldExtractor.extract(context));
    }

    /**
     * Creates a child logger with default fields from key-value pairs.
     *
     * @param kvPairs alternating key-value pairs (e.g. "key1", value1, "key2", value2)
     * @return a new Slog instance with the merged fields
     */
    public Slog with(Object... kvPairs) {
        return withFields(FieldExtractor.extractFromVarargs(kvPairs));
    }

    private Slog withFields(List<Map.Entry<String, Object>> fields) {
        var merged = new LinkedHashMap<>(defaultFields);
        for (var entry : fields) {
            merged.put(entry.getKey(), entry.getValue());
        }
        return new Slog(logger, merged);
    }

    private void log(LoggingEventBuilder builder, String msg, List<Map.Entry<String, Object>> fields) {
        for (var entry : defaultFields.entrySet()) {
            builder = builder.addKeyValue(entry.getKey(), entry.getValue());
        }
        for (var entry : fields) {
            builder = builder.addKeyValue(entry.getKey(), entry.getValue());
        }
        builder.log(msg);
    }
}
