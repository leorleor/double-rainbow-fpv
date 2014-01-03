package com.thomastechnics.abcd;


public final class OffsetDataUpdate implements DataEngine.DataUpdate {
//  private DataEngine dataEngine;
  private final String id;
  private final int offset;
  private final boolean absolute;
  private final int timeOffset;
  private int value;
  private long time;

  public OffsetDataUpdate(int offset, int value, boolean absolute) {
//  this(null, null, offset, value, absolute);
    this(null, offset, value, absolute);
  }
  public OffsetDataUpdate(String id, int offset, int value, boolean absolute) {
    this(id, offset, value, absolute, -1);
  }
  public OffsetDataUpdate(String id, int offset, int value, boolean absolute, int timeOffset) {
    this.id = id;
    this.value = value;
    this.offset = offset;
    this.absolute = absolute;
    this.timeOffset = timeOffset;
  }
  
  public String getId() {
    return id;
  }
  
  public void doUpdate(DataEngine dataEngine, float value, long time) {
    this.value = Float.floatToRawIntBits(value);
    this.time = time;
    dataEngine.update(id, this);
  }

  public void doUpdate(DataEngine dataEngine, int value, long time) {
    this.value = value;
    this.time = time;
    dataEngine.update(id, this);
  }

  @Override
  public void update(String id, ByteData data) {
    int dataValue = value;
    if (!absolute) {
      dataValue += ByteUtil.getInt(offset, data.bytes);
    }
    ByteUtil.putInt(dataValue, offset, data.bytes);
    
    if (timeOffset >= 0) {
      int timeValue = (int)(time % Integer.MAX_VALUE);
      ByteUtil.putInt(timeValue, timeOffset, data.bytes);
    }
  }
}