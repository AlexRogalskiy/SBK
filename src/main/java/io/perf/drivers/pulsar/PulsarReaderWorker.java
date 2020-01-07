/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.pulsar;

import io.perf.core.ReaderWorker;
import io.perf.core.PerfStats;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;

/**
 * Class for Pulsar reader/consumer.
 */
public class PulsarReaderWorker extends ReaderWorker {
    final private Consumer<byte[]> consumer;

    public PulsarReaderWorker(int readerId, int events, int secondsToRun,
                      long start, PerfStats stats, String streamName, String subscriptionName,
                      int timeout, boolean writeAndRead, PulsarClient client) throws  IOException {
        super(readerId, events, secondsToRun, start, stats, streamName, subscriptionName, timeout, writeAndRead);

        try {

            this.consumer = client.newConsumer()
                    .topic(streamName)
                    // Allow multiple consumers to attach to the same subscription
                    // and get messages dispatched as a queue
                    .subscriptionType(SubscriptionType.Exclusive)
                    .subscriptionName(subscriptionName)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .receiverQueueSize(1)
                    .subscribe();

        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }

    @Override
    public byte[] readData() throws IOException {
        try {
            return consumer.receive(timeout, TimeUnit.SECONDS).getData();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            consumer.close();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }
}