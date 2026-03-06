package io.github.alde.slog.format;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.encoder.EncoderBase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Logback encoder that outputs structured log events as JSON lines.
 */
public class JsonEncoder extends EncoderBase<ILoggingEvent> {

    /** Creates a new JsonEncoder. */
    public JsonEncoder() {}

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_INSTANT;

    private static final byte[] EMPTY = new byte[0];
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Override
    public byte[] headerBytes() {
        return EMPTY;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        var sb = new StringBuilder(256);
        sb.append('{');

        appendField(sb, "timestamp", formatTimestamp(event));
        appendField(sb, "level", event.getLevel().toString());
        appendField(sb, "logger", event.getLoggerName());
        appendField(sb, "message", event.getFormattedMessage());

        appendKeyValuePairs(sb, event);
        appendThrowable(sb, event);

        sb.append('}');
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
            appendField(sb, kv.key, kv.value);
        }
    }

    private void appendThrowable(StringBuilder sb, ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            appendField(sb, "error", tp.getClassName() + ": " + tp.getMessage());
            appendField(sb, "stacktrace", ThrowableProxyUtil.asString(tp));
        }
    }

    private void appendField(StringBuilder sb, String key, Object value) {
        if (sb.length() > 1) {
            sb.append(',');
        }
        sb.append('"');
        escapeJson(sb, key);
        sb.append("\":");
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append('"');
            escapeJson(sb, value.toString());
            sb.append('"');
        }
    }

    private String formatTimestamp(ILoggingEvent event) {
        return FORMATTER.format(
                Instant.ofEpochMilli(event.getTimeStamp()).atOffset(ZoneOffset.UTC));
    }

    static void escapeJson(StringBuilder sb, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u00");
                        sb.append(HEX[(c >> 4) & 0xF]);
                        sb.append(HEX[c & 0xF]);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }
}
