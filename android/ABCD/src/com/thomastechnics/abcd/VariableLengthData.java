package com.thomastechnics.abcd;


public class VariableLengthData 
//extends ByteData 
{
  public static final int HEADER_LENGTH = ByteUtil.INT_SIZE;
  
  public static ByteData createVariable(String name, boolean listen) {
    final ByteData data;
    if (listen) {
      data = new ByteData(name, 0);
    } else {
      data = new ByteData(name, HEADER_LENGTH);
      writeLength(data);
    }
    
    return data;
  }
  
  public static void writeLength(ByteData data) {
    writeLength(data.bytes, data.length);
  }
  public static void writeLength(byte[] bytes, int length) {
    ByteUtil.putInt(length, 0, bytes);
  }
  
  public static byte[] createLengthBytes() {
    byte[] header = new byte[HEADER_LENGTH];
//    ByteUtil.putInt(length, 0, header);
    return header;
  }
  
//  private int varLength;
//  
//  public VariableLengthData(String name) {
//    super(name, HEADER_LENGTH);
//  }
//
//  @Override
//  public byte[] set(byte[] bytes, int length, long timestamp) {
//    final byte[] oldBytes;
//    
//    synchronized (this) {
//      if (length > HEADER_LENGTH) {
//        oldBytes = super.set(bytes, length, timestamp);
//      } else {
//        varLength = ByteUtil.getInt(0, bytes);
//        oldBytes = bytes;
//      }
//    }
//    
//    return oldBytes; 
//  }
//
//  @Override
//  public void setLength(int length) {
//    // TODO Auto-generated method stub
//    super.setLength(length);
//  }
//  
//  
}
