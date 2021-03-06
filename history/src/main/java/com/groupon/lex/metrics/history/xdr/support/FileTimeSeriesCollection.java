package com.groupon.lex.metrics.history.xdr.support;

import com.groupon.lex.metrics.GroupName;
import com.groupon.lex.metrics.MetricName;
import com.groupon.lex.metrics.MetricValue;
import com.groupon.lex.metrics.SimpleGroupPath;
import com.groupon.lex.metrics.Tags;
import com.groupon.lex.metrics.timeseries.TimeSeriesCollection;
import com.groupon.lex.metrics.timeseries.TimeSeriesValue;
import com.groupon.lex.metrics.timeseries.TimeSeriesValueSet;
import gnu.trove.map.hash.THashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joda.time.DateTime;

/**
 *
 * @author ariane
 */
public class FileTimeSeriesCollection implements TimeSeriesCollection {
    private final DateTime timestamp_;
    private final Map<SimpleGroupPath, Map<Tags, TimeSeriesValue>> path_map_;

    private static <T> BinaryOperator<T> throwing_merger_() {
        return (x, y) -> { throw new IllegalStateException("duplicate key " + x); };
    }

    /** HashMap constructor, so we can create hashmaps with an altered load factor. */
    private static <K, V> Supplier<Map<K, V>> hashmap_constructor_() {
        return () -> new THashMap<K, V>(1, 1);
    }

    public FileTimeSeriesCollection(DateTime timestamp, Stream<TimeSeriesValue> tsv) {
        timestamp_ = timestamp;
        path_map_ = tsv.collect(
                Collectors.groupingBy(
                        v -> v.getGroup().getPath(),
                        hashmap_constructor_(),
                        Collectors.toMap(
                                v -> v.getGroup().getTags(),
                                Function.identity(),
                                throwing_merger_(),
                                hashmap_constructor_())));
    }

    @Override
    public TimeSeriesCollection add(TimeSeriesValue tsv) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public TimeSeriesCollection renameGroup(GroupName oldname, GroupName newname) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public TimeSeriesCollection addMetrics(GroupName group, Map<MetricName, MetricValue> metrics) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp_;
    }

    @Override
    public boolean isEmpty() {
        return path_map_.isEmpty();
    }

    @Override
    public Set<GroupName> getGroups() {
        return path_map_.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .map(TimeSeriesValue::getGroup)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SimpleGroupPath> getGroupPaths() {
        return path_map_.keySet();
    }

    @Override
    public TimeSeriesValueSet getTSValues() {
        return new TimeSeriesValueSet(path_map_.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream));
    }

    @Override
    public TimeSeriesValueSet getTSValue(SimpleGroupPath name) {
        return Optional.ofNullable(path_map_.get(name))
                .map(Map::values)
                .map(Collection::stream)
                .map(TimeSeriesValueSet::new)
                .orElse(TimeSeriesValueSet.EMPTY);
    }

    @Override
    public Optional<TimeSeriesValue> get(GroupName name) {
        return Optional.ofNullable(path_map_.get(name.getPath()))
                .flatMap(tag_map -> Optional.ofNullable(tag_map.get(name.getTags())));
    }

    @Override
    public TimeSeriesCollection clone() {
        return this;  // Immutable
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.timestamp_);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileTimeSeriesCollection other = (FileTimeSeriesCollection) obj;
        if (!Objects.equals(this.timestamp_, other.timestamp_)) {
            return false;
        }
        if (!Objects.equals(path_map_, other.path_map_)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FileTimeSeriesCollection{" + timestamp_ + ", " + getGroups() + '}';
    }
}
