package io.github.alde.slog;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extracts structured fields from Records, Maps, or varargs key-value pairs.
 */
final class FieldExtractor {

    private record CachedAccessor(String name, Method method) {}

    private static final ConcurrentHashMap<Class<?>, List<CachedAccessor>> ACCESSOR_CACHE =
            new ConcurrentHashMap<>();

    private FieldExtractor() {}

    static List<Map.Entry<String, Object>> extract(Object context) {
        if (context == null) {
            return Collections.emptyList();
        }
        if (context instanceof Map<?, ?> map) {
            return extractFromMap(map);
        }
        if (context instanceof Record rec) {
            return extractFromRecord(rec);
        }
        throw new IllegalArgumentException(
                "Context must be a Record or Map, got " + context.getClass().getName());
    }

    static List<Map.Entry<String, Object>> extractFromVarargs(Object[] kvPairs) {
        if (kvPairs == null || kvPairs.length == 0) {
            return Collections.emptyList();
        }
        if (kvPairs.length == 1) {
            return extract(kvPairs[0]);
        }
        if (kvPairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Key-value pairs must have an even number of arguments, got " + kvPairs.length);
        }
        var entries = new ArrayList<Map.Entry<String, Object>>(kvPairs.length / 2);
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (!(kvPairs[i] instanceof String key)) {
                String typeName = kvPairs[i] == null ? "null" : kvPairs[i].getClass().getSimpleName();
                throw new IllegalArgumentException(
                        "Keys must be strings, got " + typeName + " at index " + i);
            }
            entries.add(new AbstractMap.SimpleImmutableEntry<>(key, kvPairs[i + 1]));
        }
        return entries;
    }

    private static List<Map.Entry<String, Object>> extractFromRecord(Record rec) {
        var accessors = ACCESSOR_CACHE.computeIfAbsent(rec.getClass(), FieldExtractor::buildAccessors);
        var entries = new ArrayList<Map.Entry<String, Object>>(accessors.size());
        for (var accessor : accessors) {
            try {
                var value = accessor.method().invoke(rec);
                entries.add(new AbstractMap.SimpleImmutableEntry<>(accessor.name(), value));
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Record " + rec.getClass().getName() + " must be public to use as log context", e);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to access record component: " + accessor.name(), e);
            }
        }
        return entries;
    }

    private static List<CachedAccessor> buildAccessors(Class<?> recordClass) {
        var components = recordClass.getRecordComponents();
        var accessors = new ArrayList<CachedAccessor>(components.length);
        for (var component : components) {
            accessors.add(new CachedAccessor(component.getName(), component.getAccessor()));
        }
        return List.copyOf(accessors);
    }

    private static List<Map.Entry<String, Object>> extractFromMap(Map<?, ?> map) {
        var entries = new ArrayList<Map.Entry<String, Object>>(map.size());
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(
                        "Map keys must be strings, got " + entry.getKey().getClass().getSimpleName());
            }
            entries.add(new AbstractMap.SimpleImmutableEntry<>(key, entry.getValue()));
        }
        return entries;
    }
}
