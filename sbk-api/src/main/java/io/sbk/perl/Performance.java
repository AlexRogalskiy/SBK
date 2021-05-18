/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.perl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for performance Statistics.
 */
public interface Performance {

     /**
      * Start the performance Benchmark.
      *
      * @param secondsToRun number of seconds to Run
      * @param recordsCount Maximum number of records to count.
      *                If this value 0 or less than 0,then run the benchmark till secondsToRun.
      * @return CompletableFuture.
      * @throws IllegalStateException If an exception occurred.
      * @throws IOException If an exception occurred.
      */
     CompletableFuture<Void> start(long secondsToRun, long recordsCount) throws IOException, IllegalStateException;

     /**
      * stop/shutdown the Benchmark.
      *
      */
     void stop();

     /**
      * Get the SendChannel to get the benchmark results.
      *
      * @return SendChannel to get the benchmark results.
      */
     SendChannel getSendChannel();
}
