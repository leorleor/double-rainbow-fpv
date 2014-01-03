package com.thomastechnics.abcd;

import java.util.Arrays;

public class ByteData {
  public String name;
  byte[] bytes;
  protected int length;
  protected long timestamp;
  protected boolean variableLength;

  public ByteData() {
  }

  public ByteData(int length) {
    setLength(length);
  }

  public ByteData(String name, int length) {
    this.name = name;
    setLength(length);
  }

  public void copy(final byte[] bytes, int length, long timestamp) {
    synchronized (this) {
      setLengthAsync(length);
      System.arraycopy(bytes, 0, this.bytes, 0, length);
      this.timestamp = timestamp;
    }
  }
  public byte[] set(final byte[] bytes, int length, long timestamp) {
    final byte[] swapBytes;

    synchronized (this) {
      swapBytes = this.bytes;
      this.bytes = bytes;
      this.length = length;
      this.timestamp = timestamp;
    }

    return swapBytes;
  }

  public void swapTo(final ByteData data) {
    synchronized (this) {
      byte[] swapBytes = data.set(bytes, length, timestamp);
      bytes = lengthenAsNeeded(swapBytes, length);
    }
  }

  public void copyTo(ByteData data) {
    synchronized (this) {
      data.copy(bytes, length, timestamp);
    }
  }

  public void cloneTo(ByteData data) {
    synchronized (this) {
      data.set(bytes.clone(), length, timestamp);
    }
  }

  public long getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public int getLength() {
    return length;
  }
  public void setLength(final int length) {
    synchronized (this) {
      setLengthAsync(length);
    }
  }
  
  public boolean variableLength() {
    return variableLength;
  }

  private void setLengthAsync(final int length) {
    if (this.length != length) {
      this.length = length;
      bytes = lengthenAsNeeded(bytes, length);
    }
  }

  public static byte[] lengthenAsNeeded(byte[] bytes, int length) {
    if ((bytes == null) || (bytes.length < length)) {
      bytes = new byte[length];
    }
    return bytes;
  }

  @Override
  public String toString() {
    return name + " " + Arrays.toString(bytes);
  }
}