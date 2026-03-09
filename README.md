# java-slog

I write Go. I like Go. Go has [`slog`](https://pkg.go.dev/log/slog) and it's great - structured logging that just works, built right into the standard library.

Then I have to write Java and the options are:
- **MDC**: Global thread-local state that leaks across async boundaries, requires try-finally cleanup you'll forget, and silently carries fields from three requests ago. Fantastic.
- **Markers**: Not really structured.
- **Wrapping everything in JSON manually**: Life's too short.

So I built this. It's `slog` for Java. Or at least the parts I miss the most.

```java
var log = Slog.create(MyService.class);

// Pass a Record. Fields become key-value pairs. That's it.
record OrderCtx(String orderId, String userId, int amount) {}
log.info("order placed", new OrderCtx("ord-123", "bob", 4200));

// Or a Map, if that's your thing
log.info("inventory checked", Map.of("sku", "WIDGET-1", "inStock", true));

// Or just inline it
log.info("shipping calculated", "orderId", "ord-123", "carrier", "FedEx", "cost", 599);
```

No `MDC.put()`. No `MDC.remove()`. No try-finally. No wondering why the previous request's tenant ID is showing up in your logs at 2am.

## Child loggers

Go's `slog` has `Logger.With()`. So does this. Need the same fields on every log line for a request? Create a child logger and stop thinking about it:

```java
var reqLog = log.with("requestId", "req-abc", "tenant", "acme-corp");
reqLog.info("processing request");
// => ... processing request requestId=req-abc tenant=acme-corp

// Works with Records too
var ctxLog = log.with(new RequestCtx("req-abc", "acme-corp"));
```

Immutable. Thread-safe. No global state. No cleanup. Pass it around like a normal value, because that's what it is.

## Error logging

```java
log.error("payment failed", ex, new OrderCtx("ord-123", "bob", 4200));
log.error("payment failed", ex, "orderId", "ord-123", "reason", "insufficient funds");
```

## Output formats

Three Logback encoders. One for your terminal, one for production, one for when you really miss Go.

### Key-value

For tailing logs like a human:

```
2026-03-06 23:14:28 INFO  io.github.alde.slog.example.ExampleApp - order placed orderId=ord-123 userId=bob amount=4200
```

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="io.github.alde.slog.format.KeyValueEncoder"/>
</appender>
```

### JSON lines

For production and log aggregators:

```json
{"timestamp":"2026-03-06T23:14:28.116Z","level":"INFO","logger":"io.github.alde.slog.example.ExampleApp","message":"order placed","orderId":"ord-123","userId":"bob","amount":4200}
```

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="io.github.alde.slog.format.JsonEncoder"/>
</appender>
```

### Colorized text

Logrus-style. Because even if I can't write Go, I can at least make my Java logs *look* like Go:

```
INFO[2026-03-06T23:14:28Z] order placed orderId=ord-123 userId=bob amount=4200
```

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="io.github.alde.slog.format.ColorTextEncoder">
        <forceColor>true</forceColor> <!-- auto-detected if omitted -->
    </encoder>
</appender>
```

## Installation

### Gradle

```groovy
dependencies {
    implementation 'io.github.alde:java-slog:0.1.1'

    // Bring your own SLF4J backend
    runtimeOnly 'ch.qos.logback:logback-classic:1.5.16'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.alde</groupId>
    <artifactId>java-slog</artifactId>
    <version>0.1.1</version>
</dependency>
```

Logback is a compile-only dependency - it won't leak onto your classpath. If you use a different SLF4J backend, the `Slog` API still works; you just won't have the encoders.

## Requirements

- Java 24+
- SLF4J 2.x

## Building

```sh
./gradlew build
```

### Running the example

```sh
./gradlew :example:run                  # key-value output
./gradlew :example:run --args="--json"  # JSON output
./gradlew :example:run --args="--color" # colorized output
```

## License

[MIT](LICENSE)
