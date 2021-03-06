grammar Expression;
options {
    tokenVocab=ConfigBnf;
}
import ConfigBnf;

@parser::header {
    import com.groupon.lex.metrics.lib.TriFunction;
    import com.groupon.lex.metrics.config.*;
    import com.groupon.lex.metrics.expression.*;
    import com.groupon.lex.metrics.timeseries.*;
    import com.groupon.lex.metrics.timeseries.expression.*;
    import com.groupon.lex.metrics.NameCache;
    import com.groupon.lex.metrics.MetricValue;
    import com.groupon.lex.metrics.GroupName;
    import com.groupon.lex.metrics.PathMatcher;
    import com.groupon.lex.metrics.MetricMatcher;
    import com.groupon.lex.metrics.MetricName;
    import com.groupon.lex.metrics.SimpleGroupPath;
    import com.groupon.lex.metrics.Histogram;
    import com.groupon.lex.metrics.transformers.NameResolver;
    import com.groupon.lex.metrics.transformers.LiteralNameResolver;
    import com.groupon.lex.metrics.transformers.IdentifierNameResolver;
    import java.util.Objects;
    import java.util.SortedSet;
    import java.util.TreeSet;
    import java.util.ArrayList;
    import java.util.Collection;
    import java.util.Collections;
    import java.util.Map;
    import java.util.HashMap;
    import java.util.Deque;
    import java.util.ArrayDeque;
    import java.io.File;
    import javax.management.ObjectName;
    import javax.management.MalformedObjectNameException;
    import java.util.function.Function;
    import java.util.function.BiFunction;
    import java.util.function.Consumer;
    import java.util.Optional;
    import org.joda.time.Duration;
    import com.groupon.lex.metrics.lib.Any2;
}


expr             returns [ TimeSeriesMetricExpression s ]
                 : s1=expression EOF
                   { $s = $s1.s; }
                 ;
