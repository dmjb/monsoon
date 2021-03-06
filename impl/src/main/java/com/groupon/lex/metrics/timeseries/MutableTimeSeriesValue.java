/*
 * Copyright (c) 2016, Groupon, Inc.
 * All rights reserved. 
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. 
 *
 * Neither the name of GROUPON nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.groupon.lex.metrics.timeseries;

import com.groupon.lex.metrics.GroupName;
import com.groupon.lex.metrics.MetricName;
import com.groupon.lex.metrics.MetricValue;
import gnu.trove.map.hash.THashMap;
import static java.util.Collections.unmodifiableMap;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.function.Function;
import java.util.stream.Stream;
import org.joda.time.DateTime;

/**
 *
 * @author ariane
 */
public final class MutableTimeSeriesValue implements TimeSeriesValue {
    private DateTime timestamp_;
    private GroupName group_;
    private final Map<MetricName, MetricValue> metrics_ = new THashMap<>(4, 1);  // Favour small, dense hashmaps, since there are a lot of instances.

    public MutableTimeSeriesValue(DateTime timestamp, GroupName group) {
        timestamp_ = requireNonNull(timestamp);
        group_ = requireNonNull(group);
    }

    public MutableTimeSeriesValue(DateTime timestamp, GroupName group, Map<? extends MetricName, ? extends MetricValue> metrics) {
        this(timestamp, group, metrics.entrySet().stream(), Map.Entry::getKey, Map.Entry::getValue);
    }

    public <T> MutableTimeSeriesValue(DateTime timestamp, GroupName group, Stream<T> metric_stream, Function<T, MetricName> name_fn, Function<T, MetricValue> value_fn) {
        timestamp_ = requireNonNull(timestamp);
        group_ = requireNonNull(group);
        metric_stream.forEach((T t) -> metrics_.put(requireNonNull(name_fn.apply(t)), requireNonNull(value_fn.apply(t))));
    }

    public MutableTimeSeriesValue(TimeSeriesValue tsv) {
        this(tsv.getTimestamp(), tsv.getGroup(), tsv.getMetrics());
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp_;
    }

    public MutableTimeSeriesValue setTimestamp(DateTime timestamp) {
        timestamp_ = requireNonNull(timestamp);
        return this;
    }

    @Override
    public GroupName getGroup() {
        return group_;
    }

    public MutableTimeSeriesValue setGroup(GroupName group) {
        group_ = requireNonNull(group);
        return this;
    }

    @Override
    public Map<MetricName, MetricValue> getMetrics() {
        return unmodifiableMap(metrics_);
    }

    public MutableTimeSeriesValue addMetric(MetricName name, MetricValue value) {
        metrics_.put(name, value);
        return this;
    }

    public MutableTimeSeriesValue addMetrics(Map<? extends MetricName, ? extends MetricValue> values) {
        return addMetrics(values.entrySet().stream(), Map.Entry::getKey, Map.Entry::getValue);
    }

    public <T> MutableTimeSeriesValue addMetrics(Stream<T> values, Function<? super T, ? extends MetricName> name_fn, Function<? super T, ? extends MetricValue> value_fn) {
        values.forEach(v -> addMetric(requireNonNull(name_fn.apply(v)), requireNonNull(value_fn.apply(v))));
        return this;
    }

    @Override
    public String toString() {
        return "TimeSeriesValue{" + "timestamp=" + timestamp_ + ", group=" + group_ + ", metrics=" + metrics_ + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.group_);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TimeSeriesValue)) {
            return false;
        }
        final TimeSeriesValue other = (TimeSeriesValue) obj;
        if (!Objects.equals(this.getTimestamp(), other.getTimestamp())) {
            return false;
        }
        if (!Objects.equals(this.getGroup(), other.getGroup())) {
            return false;
        }
        if (!Objects.equals(this.getMetrics(), other.getMetrics())) {
            return false;
        }
        return true;
    }

    @Override
    public MutableTimeSeriesValue clone() {
        return new MutableTimeSeriesValue(this);
    }
}
