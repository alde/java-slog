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
import java.util.Map;

/**
 * Logback encoder that outputs structured log events in key=value format.
 */
public class KeyValueEncoder extends EncoderBase<ILoggingEvent> {

    /** Creates a new KeyValueEncoder. */
    public KeyValueEncoder() {}

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final byte[] EMPTY = new byte[0];

    private static final Map<Level, String> PADDED_LEVELS = Map.of(
            Level.TRACE, "TRACE",
            Level.DEBUG, "DEBUG",
            Level.INFO,  "INFO ",
            Level.WARN,  "WARN ",
            Level.ERROR, "ERROR"
    );

    @Override
    public byte[] headerBytes() {
        return EMPTY;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        var sb = new StringBuilder(256);

        sb.append(FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
        sb.append(' ');
        sb.append(PADDED_LEVELS.getOrDefault(event.getLevel(), event.getLevel().toString()));
        sb.append(' ');
        sb.append(event.getLoggerName());
        sb.append(" - ");
        sb.append(event.getFormattedMessage());

        appendKeyValuePairs(sb, event);
        appendThrowable(sb, event);

        sb.append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return EMPTY;
    }

    private void appendKeyValuePairs(StringBuilder sb, ILoggingEvent event) {
        var kvPairs = event.getKeyValuePairs();
        if (kvPairs == null) return;
        for (var kv : kvPairs) {
            sb.append(' ');
            sb.append(kv.key);
            sb.append('=');
            appendValue(sb, kv.value);
        }
    }

    private void appendThrowable(StringBuilder sb, ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            sb.append('\n');
            sb.append(ThrowableProxyUtil.asString(tp));
        }
    }

    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        String str = value.toString();
        boolean needsQuoting = str.isEmpty();
        for (int i = 0; !needsQuoting && i < str.length(); i++) {
            char c = str.charAt(i);
            needsQuoting = c == ' ' || c == '=' || c == '"' || c == '\\' || c == '\n' || c == '\r' || c == '\t';
        }
        if (needsQuoting) {
            sb.append('"');
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '\\' -> sb.append("\\\\");
                    case '"' -> sb.append("\\\"");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            sb.append('"');
        } else {
            sb.append(str);
        }
    }
}
