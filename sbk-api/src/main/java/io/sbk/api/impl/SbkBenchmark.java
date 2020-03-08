/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.api.Benchmark;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Performance;
import io.sbk.api.QuadConsumer;
import io.sbk.api.Reader;
import io.sbk.api.ResultLogger;
import io.sbk.api.Storage;
import io.sbk.api.Writer;
import lombok.Synchronized;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SbkBenchmark implements Benchmark {
    final private String action;
    final private int reportingInterval;
    final private Storage storage;
    final private ExecutorService executor;
    final private Parameters params;
    final private Performance writeStats;
    final private Performance readStats;
    final private QuadConsumer writeTime;
    final private QuadConsumer readTime;
    final private ResultLogger metricsLogger;
    final private ResultLogger logger;
    final private ScheduledExecutorService timeoutExecutor;
    final private CompletableFuture<Void> ret;
    private List<Writer> writers;
    private List<Reader> readers;
    private List<SbkWriter> sbkWriters;
    private List<SbkReader> sbkReaders;
    private List<Callable<Void>> workers;
    private boolean state;

    public SbkBenchmark(String  action, Parameters params, Storage storage, ResultLogger logger,
                        ResultLogger metricsLogger, int reportingInterval, ExecutorService executor) {
        this.action = action;
        this.params = params;
        this.storage = storage;
        this.logger = logger;
        this.metricsLogger = logger;
        this.reportingInterval = reportingInterval;
        this.executor = executor;
         if (params.getWritersCount() > 0 && !params.isWriteAndRead()) {
            writeStats = new SbkPerformance(action, reportingInterval, params.getRecordSize(),
                    params.getCsvFile(), metricsLogger, logger, executor);
            writeTime = writeStats::recordTime;
        } else {
            writeStats = null;
            writeTime = null;
        }

        if (params.getReadersCount() > 0) {
            readStats = new SbkPerformance(action, reportingInterval, params.getRecordSize(),
                    params.getCsvFile(), metricsLogger, logger, executor);
            readTime = readStats::recordTime;
        } else {
            readStats = null;
            readTime = null;
        }
        ret = new CompletableFuture<>();
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        state = false;
    }

    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IOException, IllegalStateException {
        if (state) {
            throw  new IllegalStateException("SbkBenchmark is already started\n");
        }
        storage.openStorage(params);
        final DataType data = storage.getDataType();
        writers = IntStream.range(0, params.getWritersCount())
                .boxed()
                .map(i -> storage.createWriter(i, params))
                .collect(Collectors.toList());

        readers = IntStream.range(0, params.getReadersCount())
                .boxed()
                .map(i -> storage.createReader(i, params))
                .collect(Collectors.toList());

        sbkWriters =  IntStream.range(0, params.getWritersCount())
                .boxed()
                .map(i -> new SbkWriter(i, params, writeTime, data, writers.get(i)))
                .collect(Collectors.toList());

        sbkReaders = IntStream.range(0, params.getReadersCount())
                .boxed()
                .map(i -> new SbkReader(i, params, readTime, data, readers.get(i)))
                .collect(Collectors.toList());

        workers = Stream.of(sbkReaders, sbkWriters)
                .filter(x -> x != null)
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
        final long startTime = System.currentTimeMillis();
        if (writeStats != null && !params.isWriteAndRead()) {
            writeStats.start(startTime);
        }
        if (readStats != null) {
            readStats.start(startTime);
        }
        workers.forEach(executor::submit);
        if (params.getSecondsToRun() > 0) {
            timeoutExecutor.schedule(this::stop, params.getSecondsToRun(), TimeUnit.SECONDS);
        }
        state = true;
        return ret;
    }

    @Override
    @Synchronized
    public void stop() {
        if (ret.isDone()) {
            return;
        }
        try {
            if (writeStats != null && !params.isWriteAndRead()) {
                writeStats.shutdown(System.currentTimeMillis());
            }
            if (readStats != null) {
                readStats.shutdown(System.currentTimeMillis());
            }
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
        if (readers != null) {
            readers.forEach(c -> {
                try {
                    c.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
        if (writers != null) {
            writers.forEach(c -> {
                try {
                    c.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
        try {
            storage.closeStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        ret.complete(null);
    }
}
