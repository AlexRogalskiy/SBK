/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FileStream;
import io.sbk.data.DataType;
import io.sbk.api.ParameterOptions;
import io.sbk.perl.SendChannel;
import io.sbk.api.Status;
import io.sbk.time.Time;
import io.sbk.api.Writer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for File Writer.
 */
public class FileStreamWriter implements Writer<byte[]> {
    final private FileOutputStream out;

    public FileStreamWriter(int id, ParameterOptions params, FileStreamConfig config) throws IOException {
        this.out = new FileOutputStream(config.fileName, config.isAppend);
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time, Status status, SendChannel record, int id) throws IOException {
        status.startTime = time.getCurrentTime();
        out.write(data);
        status.endTime = time.getCurrentTime();
        status.records = 1;
        status.bytes = size;
        record.send(id, status.startTime, status.endTime, size, 1);
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        out.write(data);
        return null;
    }

    @Override
    public void sync() throws IOException {
        out.flush();
        out.getFD().sync();
    }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}