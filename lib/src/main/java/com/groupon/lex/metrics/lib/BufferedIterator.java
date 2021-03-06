package com.groupon.lex.metrics.lib;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.Spliterator;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;

public final class BufferedIterator<T> {
    private static final Logger LOG = Logger.getLogger(BufferedIterator.class.getName());
    private final ExecutorService work_queue_;
    private final Iterator<? extends T> iter_;
    private final List<T> queue_;
    private Exception exception = null;
    private boolean at_end_;
    private final int queue_size_;
    private boolean running_ = false;
    private Optional<Runnable> wakeup_ = Optional.empty();

    public BufferedIterator(ExecutorService work_queue, Iterator<? extends T> iter, int queue_size) {
        if (queue_size <= 0) throw new IllegalArgumentException("queue size must be at least 1");
        work_queue_ = requireNonNull(work_queue);
        iter_ = requireNonNull(iter);
        queue_size_ = queue_size;
        queue_ = new LinkedList<>();
        at_end_ = !iter_.hasNext();

        if (iter_.hasNext()) fire_();
    }

    public BufferedIterator(ExecutorService work_queue, Iterator<? extends T> iter) {
        this(work_queue, iter, 16);
    }

    /** Adapt the IterQueue as a blocking iterator. */
    public Iterator<T> asIterator() {
        return new BlockingIterator();
    }

    /** Adapt the IterQueue as a blocking stream. */
    public Stream<T> asStream() {
        final Spliterator<T> spliter = Spliterators.spliteratorUnknownSize(asIterator(), NONNULL | IMMUTABLE | ORDERED);
        return StreamSupport.stream(spliter, false);
    }

    public static <T> Iterator<T> iterator(ExecutorService work_queue, Iterator<? extends T> iter, int queue_size) {
        return new BufferedIterator<>(work_queue, iter, queue_size).asIterator();
    }
    public static <T> Iterator<T> iterator(ExecutorService work_queue, Iterator<? extends T> iter) {
        return new BufferedIterator<>(work_queue, iter).asIterator();
    }

    public static <T> Stream<T> stream(ExecutorService work_queue, Iterator<? extends T> iter, int queue_size) {
        return new BufferedIterator<>(work_queue, iter, queue_size).asStream();
    }
    public static <T> Stream<T> stream(ExecutorService work_queue, Iterator<? extends T> iter) {
        return new BufferedIterator<>(work_queue, iter).asStream();
    }
    public static <T> Stream<T> stream(ExecutorService work_queue, Stream<? extends T> stream, int queue_size) {
        return stream(work_queue, stream.iterator(), queue_size);
    }
    public static <T> Stream<T> stream(ExecutorService work_queue, Stream<? extends T> stream) {
        return stream(work_queue, stream.iterator());
    }

    public synchronized boolean atEnd() {
        return at_end_ && queue_.isEmpty() && exception == null;
    }

    public synchronized boolean nextAvail() {
        return !queue_.isEmpty() || exception != null;
    }

    @SneakyThrows
    public synchronized T next() {
        if (exception != null) throw exception;
        try {
            final T result = queue_.remove(0);
            fire_();
            return result;
        } catch (IndexOutOfBoundsException ex) {
            LOG.log(Level.SEVERE, "next() called on empty queue!", ex);
            throw ex;
        }
    }

    public void setWakeup(Runnable wakeup) {
        requireNonNull(wakeup);
        boolean run_immediately_ = false;
        synchronized(this) {
            if (!queue_.isEmpty() || at_end_) {
                run_immediately_ = true;
                wakeup_ = Optional.empty();
            } else {
                wakeup_ = Optional.of(wakeup);
            }
        }
        if (run_immediately_) work_queue_.submit(wakeup);
    }

    private synchronized void fire_() {
        if (at_end_) return;
        if (queue_.size() >= queue_size_) return;
        if (exception != null) return;

        if (!running_) {
            running_ = true;
            work_queue_.submit(this::add_next_iter_);
        }
    }

    private void add_next_iter_() {
        try {
            int count_down = queue_size_;
            boolean stop_loop = false;
            while (!stop_loop && count_down-- > 0 && queue_.size() < queue_size_) {
                {
                    final T next = iter_.next();
                    final Optional<Runnable> wakeup;
                    synchronized(this) {
                        queue_.add(next);
                        wakeup = wakeup_;
                        wakeup_ = Optional.empty();
                    }
                    wakeup.ifPresent(Runnable::run);
                }

                if (!iter_.hasNext()) {
                    final Optional<Runnable> wakeup;
                    synchronized(this) {
                        at_end_ = true;
                        stop_loop = true;
                        wakeup = wakeup_;
                        wakeup_ = Optional.empty();
                    }
                    wakeup.ifPresent(Runnable::run);
                }
            }

            synchronized(this) {
                running_ = false;
                fire_();
            }
        } catch (Exception e) {
            final Optional<Runnable> wakeup;
            synchronized(this) {
                running_ = false;
                exception = e;
                wakeup = wakeup_;
                wakeup_ = Optional.empty();
            }
            wakeup.ifPresent(Runnable::run);
        }
    }

    private class BlockingIterator implements Iterator<T> {
        @Override
        public boolean hasNext() {
            wait_();
            assert(BufferedIterator.this.nextAvail() || BufferedIterator.this.atEnd());
            return !BufferedIterator.this.atEnd();
        }

        @Override
        public T next() {
            wait_();
            if (BufferedIterator.this.atEnd()) throw new NoSuchElementException();
            assert(BufferedIterator.this.nextAvail());
            return BufferedIterator.this.next();
        }

        /**
         * Wait for a new element to become available.
         * May be interrupted, in which case it will return before the wakeup arrives.
         */
        private void wait_() {
            while (!(BufferedIterator.this.nextAvail() || BufferedIterator.this.atEnd())) {
                synchronized(this) {
                    BufferedIterator.this.setWakeup(() -> {
                        synchronized(BlockingIterator.this) {
                            BlockingIterator.this.notify();
                        }
                    });
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(BufferedIterator.class.getName()).log(Level.WARNING, "interrupted wait", ex);
                    }
                }
            }
        }
    }
}
