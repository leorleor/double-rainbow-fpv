package com.thomastechnics.abcd;


public final class ArrayDataUpdate implements DataEngine.DataUpdate {
//  private DataEngine dataEngine;
  private final String id;
  private final int offset;
  private final boolean absolute;
  private final int timeOffset;
  private int[] values;
  private long time;

//  public ArrayDataUpdate(int offset, int[] values, boolean absolute) {
////  this(null, null, offset, value, absolute);
//    this(null, offset, values, absolute);
//  }
//  public ArrayDataUpdate(String id, int offset, int[] values, boolean absolute) {
//    this(id, offset, values, absolute, -1);
//  }
  public ArrayDataUpdate(String id, int offset, int values[], boolean absolute, int timeOffset) {
    this.id = id;
    this.values = values;
    this.offset = offset;
    this.absolute = absolute;
    this.timeOffset = timeOffset;
  }
  
  public String getId() {
    return id;
  }
  
  public void doUpdate(DataEngine dataEngine, long time) {
    this.time = time;
    dataEngine.update(id, this);
  }

  @Override
  public void update(String id, ByteData data) {
    for (int index = 0; index < values.length; ++index) {
      int value = values[index];
      int valueOffset = offset + index * ByteUtil.INT_SIZE;
      if (!absolute) {
        value += ByteUtil.getInt(valueOffset, data.bytes);
      }
      ByteUtil.putInt(value, valueOffset, data.bytes);
    }
    
    if (timeOffset >= 0) {
      int timeValue = (int)(time % Integer.MAX_VALUE);
      ByteUtil.putInt(timeValue, timeOffset, data.bytes);
      data.timestamp = Math.max(data.timestamp, time);
    }
  }
  
//  public static void doCommandUpdate(DataEngine dataEngine, String id, int command, int data, long time) {
//    int[] values = {
//        command,
//        data,
//    };
//    ArrayDataUpdate update = new ArrayDataUpdate(DataId.CONTROL, DataId.COMMAND, values, true, DataId.TIMESTAMP);
//    update.doUpdate(dataEngine, time);
//  }
//  public static void onCommandUpdate(DataEngine dataEngine, int command, int data, long time) {
//    OffsetDataUpdate update = new OffsetDataUpdate(DataId.MODEL_SETUP, command, 0, true, DataId.TIMESTAMP);
//    update.doUpdate(dataEngine, data, time);
//  }
}