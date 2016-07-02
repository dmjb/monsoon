package com.groupon.lex.metrics.value;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Models a single metric value.
 *
 * A metric value can be:
 * - null (no value present)
 * - an integer type
 * - a floating point type
 * - a string
 *
 * Because this class needs to be serializable using the JMX MXBean spec, it
 * encodes each type separately, using null values to fill in absent types.
 *
 * MetricValueAdapter is an immutable class.
 * @author ariane
 */
public interface MetricValue {
    default Optional<Boolean> asBoolean() {return Optional.empty();}
    default Optional<Long> asLong() {return Optional.empty();}
    default Optional<Double> asDouble() {return Optional.empty();}
    default Optional<String> asString() {return Optional.empty();}

    default MetricValue whenBooleanDo(Consumer<Boolean> fn) {
        final Optional<Boolean> val = asBoolean();
        if (val.isPresent()) {
            fn.accept(val.get());
        }
        return this;
    }

    default MetricValue whenLongDo(Consumer<Long> fn) {
        final Optional<Long> val = asLong();
        if (val.isPresent()) {
            fn.accept(val.get());
        }
        return this;
    }

    default MetricValue whenDoubleDo(Consumer<Double> fn) {
        final Optional<Double> val = asDouble();
        if (val.isPresent()) {
            fn.accept(val.get());
        }
        return this;
    }

    default MetricValue whenStringDo(Consumer<String> fn) {
        final Optional<String> val = asString();
        if (val.isPresent()) {
            fn.accept(val.get());
        }
        return this;
    }

    default MetricValue whenEmptyDo(Runnable fn) {
        final Optional<String> val = asString();
        if (val.isPresent()) {
            fn.run();
        }
        return this;
    }

    default String configString() {return null;}
    default boolean isEmpty() {return false;}
}