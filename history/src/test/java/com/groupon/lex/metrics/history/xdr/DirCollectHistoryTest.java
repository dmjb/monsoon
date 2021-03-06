/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupon.lex.metrics.history.xdr;

import com.groupon.lex.metrics.history.xdr.support.FileSupport;
import com.groupon.lex.metrics.timeseries.TimeSeriesCollection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.util.Collections.reverse;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ariane
 */
public class DirCollectHistoryTest {
    private static final long LIMIT = 16l * 1024 * 1024;
    private static final long MAX_FILESIZE = 4l * 1024 * 1024;
    private final FileSupport file_support = new FileSupport(Const.MAJOR, Const.MINOR);
    private Path tmpdir;
    private DirCollectHistory hist;

    @Before
    public void setup() throws Exception {
        tmpdir = Files.createTempDirectory("monsoon-DirCollectHistoryTest");
        tmpdir.toFile().deleteOnExit();
        hist = new DirCollectHistory(tmpdir, LIMIT, MAX_FILESIZE);
    }

    @After
    public void cleanup() throws Exception {
        Files.list(tmpdir)
                .forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException ex) {
                        Logger.getLogger(DirCollectHistoryTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        Files.delete(tmpdir);

        hist = null;
    }

    @Test
    public void size_stays_below_threshold() throws Exception {
        final Iterator<TimeSeriesCollection> iter = create_tsdata_().iterator();
        // First, grow until 90%, to get an estimate of file size usage.
        int count_until_90_percent = 0;
        while (getTmpdirSize() < 9 * LIMIT / 10) {
            for (int i = 0; i < TSDataFileChainTest.LARGE_COUNT; ++i) {
                ++count_until_90_percent;
                hist.add(iter.next());
            }
        }
        System.out.println("90% threshold = " + count_until_90_percent);
        // Secondly, add the same amount again.
        for (int i = 0; i < count_until_90_percent; ++i) {
            hist.add(iter.next());
            assertThat(getTmpdirSize(), Matchers.lessThanOrEqualTo(LIMIT));
        }
    }

    @Test
    public void add() throws Exception {
        hist.add(create_tsdata_().findFirst().get());

        assertEquals(hist.stream().findFirst().get(), create_tsdata_().findFirst().get());
    }

    @Test
    public void add_all() throws Exception {
        List<TimeSeriesCollection> tsdata = create_tsdata_().limit(100).collect(Collectors.toList());
        hist.addAll(tsdata);

        assertEquals(hist.stream().collect(Collectors.toList()), tsdata);
    }

    @Test
    public void reversed() throws Exception {
        List<TimeSeriesCollection> tsdata = create_tsdata_().limit(100).collect(Collectors.toList());
        hist.addAll(tsdata);
        reverse(tsdata);

        assertEquals(hist.streamReversed().collect(Collectors.toList()), tsdata);
    }

    /** Get the size of all files in tmpdir. */
    private long getTmpdirSize() throws Exception {
        try (Stream<Path> listing = Files.list(tmpdir)) {
            return listing
                    .mapToLong(fname -> {
                        try {
                            return Files.size(fname);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .sum();
        }
    }

    private Stream<TimeSeriesCollection> create_tsdata_() {
        return file_support.create_tsdata(10000);
    }
}
