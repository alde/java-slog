package io.github.alde.slog.format;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.encoder.EncoderBase;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Logback encoder that outputs colorized structured log events in a
 * Logrus-inspired text format. Colors can be disabled via the
 * {@code forceColor} property or are auto-detected based on terminal support.
 *
 * <pre>
 * INFO[2024-01-15T10:30:00Z] order placed         orderId=123 userId=bob
 * WARN[2024-01-15T10:30:01Z] rate limit approaching currentRate=95
 * ERRO[2024-01-15T10:30:02Z] payment failed        error="insufficient funds"
 * </pre>
 */
public class ColorTextEncoder extends EncoderBase<ILoggingEvent> {

    /** Creates a new ColorTextEncoder. */
    public ColorTextEncoder() {}

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static final byte[] EMPTY = new byte[0];

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String CYAN = "\033[36m";
    private static final String YELLOW = "\033[33m";
    private static final String RED = "\033[31m";
    private static final String BLUE = "\033[34m";
    private static final String GRAY = "\033[90m";

    private Boolean forceColor;

    /**
     * Set to {@code true} to always emit ANSI colors, or {@code false} to
     * never emit them. When unset, colors are auto-detected.
     *
     * @param forceColor whether to force color output
     */
    public void setForceColor(boolean forceColor) {
        this.forceColor = forceColor;
    }

    @Override
    public byte[] headerBytes() {
        return EMPTY;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        boolean color = isColorEnabled();
        var sb = new StringBuilder(256);

        String levelAbbrev = abbreviateLevel(event.getLevel());
        String timestamp = FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()));

        if (color) {
            sb.append(BOLD);
            sb.append(colorForLevel(event.getLevel()));
        }
        sb.append(levelAbbrev);
        if (color) sb.append(RESET);

        if (color) sb.append(GRAY);
        sb.append('[');
        sb.append(timestamp);
        sb.append(']');
        if (color) sb.append(RESET);

        sb.append(' ');
        sb.append(event.getFormattedMessage());

        appendKeyValuePairs(sb, event, color);
        appendThrowable(sb, event, color);

        sb.append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return EMPTY;
    }

    private void appendKeyValuePairs(StringBuilder sb, ILoggingEvent event, boolean color) {
        var kvPairs = event.getKeyValuePairs();
        if (kvPairs == null) return;
        for (var kv : kvPairs) {
            sb.append(' ');
            if (color) sb.append(CYAN);
            sb.append(kv.key);
            if (color) sb.append(RESET);
            sb.append('=');
            appendValue(sb, kv.value);
        }
    }

    private void appendThrowable(StringBuilder sb, ILoggingEvent event, boolean color) {
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            sb.append('\n');
            if (color) sb.append(RED);
            sb.append(ThrowableProxyUtil.asString(tp));
            if (color) sb.append(RESET);
        }
    }

    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        String str = value.toString();
        if (str.indexOf(' ') >= 0 || str.indexOf('=') >= 0 || str.isEmpty()) {
            sb.append('"');
            sb.append(str.replace("\\", "\\\\").replace("\"", "\\\""));
            sb.append('"');
        } else {
            sb.append(str);
        }
    }

    private boolean isColorEnabled() {
        if (forceColor != null) {
            return forceColor;
        }
        return System.console() != null;
    }

    private static String abbreviateLevel(Level level) {
        return switch (level.toInt()) {
            case Level.TRACE_INT -> "TRAC";
            case Level.DEBUG_INT -> "DEBU";
            case Level.WARN_INT -> "WARN";
            case Level.ERROR_INT -> "ERRO";
            default -> "INFO";
        };
    }

    private static String colorForLevel(Level level) {
        return switch (level.toInt()) {
            case Level.TRACE_INT -> GRAY;
            case Level.DEBUG_INT -> GRAY;
            case Level.WARN_INT -> YELLOW;
            case Level.ERROR_INT -> RED;
            default -> BLUE;
        };
    }
}
