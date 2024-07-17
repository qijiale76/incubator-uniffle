/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.serializer.writable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.WritableUtils;

import org.apache.uniffle.common.serializer.SerializationStream;

public class RawWritableSerializationStream<K, V> extends SerializationStream {

  public static final int EOF_MARKER = -1; // End of File Marker

  private DataOutputStream dataOut;
  // DataOutputStream::size return int, can not support big file which is larger than
  // Integer.MAX_VALUE.
  // Here introduce totalBytesWritten to record the written bytes.
  private long totalBytesWritten = 0;

  public RawWritableSerializationStream(WritableSerializerInstance instance, OutputStream out) {
    if (out instanceof DataOutputStream) {
      dataOut = (DataOutputStream) out;
    } else {
      dataOut = new DataOutputStream(out);
    }
  }

  @Override
  public void writeRecord(Object key, Object value) throws IOException {
    DataOutputBuffer keyBuffer = (DataOutputBuffer) key;
    DataOutputBuffer valueBuffer = (DataOutputBuffer) value;
    int keyLength = keyBuffer.getLength();
    int valueLength = valueBuffer.getLength();
    // write size and buffer to output
    WritableUtils.writeVInt(dataOut, keyLength);
    WritableUtils.writeVInt(dataOut, valueLength);
    keyBuffer.writeTo(dataOut);
    valueBuffer.writeTo(dataOut);
    totalBytesWritten +=
        WritableUtils.getVIntSize(keyLength)
            + WritableUtils.getVIntSize(valueLength)
            + keyBuffer.getLength()
            + valueBuffer.getLength();
  }

  @Override
  public void flush() throws IOException {
    dataOut.flush();
  }

  @Override
  public void close() throws IOException {
    if (dataOut != null) {
      WritableUtils.writeVInt(dataOut, EOF_MARKER);
      WritableUtils.writeVInt(dataOut, EOF_MARKER);
      totalBytesWritten += 2 * WritableUtils.getVIntSize(EOF_MARKER);
      dataOut.close();
      dataOut = null;
    }
  }

  @Override
  public long getTotalBytesWritten() {
    return totalBytesWritten;
  }
}
