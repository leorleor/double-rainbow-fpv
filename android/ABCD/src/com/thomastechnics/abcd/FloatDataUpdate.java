package com.thomastechnics.abcd;


public final class FloatDataUpdate implements DataEngine.DataUpdate {
//  private DataEngine dataEngine;
  private final String id;
  private float value;
  private final int offset;
  private final boolean absolute;

  public FloatDataUpdate(int offset, float value, boolean absolute) {
//  this(null, null, offset, value, absolute);
    this(null, offset, value, absolute);
  }
  public FloatDataUpdate(String id, int offset, float value, boolean absolute) {
// }
//  public FloatDataUpdate(DataEngine dataEngine, String id, int offset, float value, boolean absolute) {
//    this.dataEngine = dataEngine;
    this.id = id;
    this.value = value;
    this.offset = offset;
    this.absolute = absolute;
  }
  
  public String getId() {
    return id;
  }
  
  public void doUpdate(DataEngine dataEngine, float value) {
    this.value = value;
    dataEngine.update(id, this);
  }

  @Override
  public void update(String id, ByteData data) {
    float dataValue = value;
    if (!absolute) {
      dataValue += ByteUtil.getFloat(offset, data.bytes);
      dataValue = Math.min(1, Math.max(-1, dataValue));
    }
    ByteUtil.putFloat(dataValue, offset, data.bytes);
  }
}