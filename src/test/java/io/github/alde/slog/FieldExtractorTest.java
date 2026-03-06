package io.github.alde.slog;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldExtractorTest {

    record User(String name, int age) {}
    record Empty() {}

    @Test
    void extractsFieldsFromRecord() {
        var fields = FieldExtractor.extract(new User("alice", 30));

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getKey()).isEqualTo("name");
        assertThat(fields.get(0).getValue()).isEqualTo("alice");
        assertThat(fields.get(1).getKey()).isEqualTo("age");
        assertThat(fields.get(1).getValue()).isEqualTo(30);
    }

    @Test
    void extractsFieldsFromEmptyRecord() {
        var fields = FieldExtractor.extract(new Empty());
        assertThat(fields).isEmpty();
    }

    @Test
    void extractsFieldsFromMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("orderId", "123");
        map.put("amount", 42);

        var fields = FieldExtractor.extract(map);

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getKey()).isEqualTo("orderId");
        assertThat(fields.get(0).getValue()).isEqualTo("123");
        assertThat(fields.get(1).getKey()).isEqualTo("amount");
        assertThat(fields.get(1).getValue()).isEqualTo(42);
    }

    @Test
    void extractsFromVarargKeyValuePairs() {
        var fields = FieldExtractor.extractFromVarargs(
                new Object[]{"orderId", "123", "amount", 42});

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getKey()).isEqualTo("orderId");
        assertThat(fields.get(0).getValue()).isEqualTo("123");
        assertThat(fields.get(1).getKey()).isEqualTo("amount");
        assertThat(fields.get(1).getValue()).isEqualTo(42);
    }

    @Test
    void singleVarargDelegatesToExtract() {
        var fields = FieldExtractor.extractFromVarargs(
                new Object[]{new User("bob", 25)});

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getKey()).isEqualTo("name");
        assertThat(fields.get(0).getValue()).isEqualTo("bob");
    }

    @Test
    void rejectsOddNumberOfVarargs() {
        assertThatThrownBy(() -> FieldExtractor.extractFromVarargs(
                new Object[]{"key1", "val1", "orphan"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("even number");
    }

    @Test
    void rejectsNonStringKeys() {
        assertThatThrownBy(() -> FieldExtractor.extractFromVarargs(
                new Object[]{42, "val"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Keys must be strings");
    }

    @Test
    void rejectsUnsupportedContextType() {
        assertThatThrownBy(() -> FieldExtractor.extract("not a record or map"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a Record or Map");
    }

    @Test
    void rejectsPlainObjectContext() {
        assertThatThrownBy(() -> FieldExtractor.extract(42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a Record or Map");
    }

    @Test
    void returnsEmptyForNull() {
        assertThat(FieldExtractor.extract(null)).isEmpty();
        assertThat(FieldExtractor.extractFromVarargs(null)).isEmpty();
        assertThat(FieldExtractor.extractFromVarargs(new Object[]{})).isEmpty();
    }

    @Test
    void rejectsNonStringMapKeys() {
        var map = new java.util.HashMap<>();
        map.put(42, "value");

        assertThatThrownBy(() -> FieldExtractor.extract(map))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map keys must be strings");
    }

    @Test
    void rejectsNullVarargKey() {
        assertThatThrownBy(() -> FieldExtractor.extractFromVarargs(
                new Object[]{null, "value"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Keys must be strings");
    }
}
