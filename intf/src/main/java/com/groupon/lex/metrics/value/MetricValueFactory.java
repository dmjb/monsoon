package com.groupon.lex.metrics.value;

import com.groupon.lex.metrics.ConfigSupport;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.Data;

public class MetricValueFactory {
    @Data
    private static class MetricBoolean implements MetricValue {
        private final boolean value;

        @Override
        public Optional<Boolean> asBoolean() {
            return Optional.of(value);
        }

        @Override
        public String configString() {
            return Boolean.toString(value);
        }
    }

    @Data
    private static class MetricLong implements MetricValue {
        private final long value;

        @Override
        public Optional<Long> asLong() {
            return Optional.of(value);
        }

        @Override
        public String configString() {
            return Long.toString(value);
        }
    }

    @Data
    private static class MetricDouble implements MetricValue {
        private final double value;

        @Override
        public Optional<Double> asDouble() {
            return Optional.of(value);
        }

        @Override
        public String configString() {
            return Double.toString(value);
        }
    }

    @Data
    private static class MetricString implements MetricValue {
        private final String value;

        @Override
        public Optional<String> asString() {
            return Optional.of(value);
        }

        @Override
        public String configString() {
            return ConfigSupport.quotedString(value).toString();
        }
    }

    @Data
    private static class EmptyMetric implements MetricValue {
        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    private static final MetricBoolean TRUE = new MetricBoolean(true);
    private static final MetricBoolean FALSE = new MetricBoolean(false);
    public static final MetricValue EMPTY = new EmptyMetric();

    private static MetricValue emptyOr(Object value, Supplier<MetricValue> supplier) {
        return (value == null) ? EMPTY : supplier.get();
    }

    public static MetricValue from(Boolean value) {
        return emptyOr(value, () -> value ? TRUE : FALSE);
    }

    public static MetricValue from(Long value) {
        return emptyOr(value, () -> new MetricLong(value));
    }

    public static MetricValue from(Double value) {
        return emptyOr(value, () -> new MetricDouble(value));
    }

    public static MetricValue from(String value) {
        return emptyOr(value, () -> new MetricString(value));
    }

    public static MetricValue from(Number number) {
        return emptyOr(number, () -> {
            // Numbers are: AtomicInteger, AtomicLong, BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, Short
            if (number instanceof Float || number instanceof Double || number instanceof BigDecimal) {
                return new MetricDouble(number.doubleValue());
            } else {
                return new MetricLong(number.longValue());
            }
        });
    }
}