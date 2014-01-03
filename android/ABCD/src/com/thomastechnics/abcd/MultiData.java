package com.thomastechnics.abcd;

public class MultiData extends ByteData {

  private DataEngine dataEngine;
  private String[] multiIds;
  private final ByteData swapBuffer = new ByteData();
  
  public MultiData(DataEngine dataEngine, String[] multiIds) {
    super();
    this.dataEngine = dataEngine;
    this.multiIds = multiIds;
  }

  @Override
  public byte[] set(byte[] bytes, int length, long timestamp) {
    int index = 0;
    for (String multiId : multiIds) {
      int multiLength = dataEngine.get(multiId).getLength();
      byte[] multiBytes = new byte[multiLength];
      System.arraycopy(bytes, index, multiBytes, 0, multiBytes.length);
      dataEngine.set(multiBytes, multiBytes.length, timestamp, multiId);
      index += multiBytes.length;
    }
    return super.set(bytes, length, timestamp);
  }

  @Override
  public void swapTo(ByteData data) {
    int index = 0;
    for (String multiId : multiIds) {
      dataEngine.get(multiId).copyTo(swapBuffer);
      System.arraycopy(swapBuffer.bytes, 0, bytes, index, swapBuffer.length);
      index += swapBuffer.length;
    }
    super.swapTo(data);
  }
}
