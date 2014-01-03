package com.thomastechnics.abcd;

import java.io.UnsupportedEncodingException;

public class ByteUtil {
  public static final String BYTE_CHARSET = "UTF-8";
  public static final byte ASCII_ZERO = (byte) Character.forDigit(0, 10);
  
  public static String getString(byte[] fromBytes, int offset, int length) {
    final String toString;

    try {
      toString = new String(fromBytes, offset, length, BYTE_CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    return toString;
  }

  public static byte[] putOrCopyString(String fromString, byte[] toBytes) {
    byte[] fromBytes;
    
    try {
      fromBytes = fromString.getBytes(BYTE_CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    
    if (toBytes.length == fromBytes.length) {
      System.arraycopy(fromBytes, 0, toBytes, 0, fromBytes.length);
    } else {
      toBytes = fromBytes;
    }
    
    return fromBytes;
  }    

  public static void getFloats(float[] toFloats, byte[] fromBytes) {
    getFloats(toFloats, 0, fromBytes);
  }    

  public static void getFloats(float[] toFloats, int offset, byte[] fromBytes) {
    for (int floatInd = 0; floatInd < toFloats.length; ++floatInd) {
      final int fromInt = getInt(offset + floatInd * 4, fromBytes);
      float toFloat = Float.intBitsToFloat(fromInt);
      toFloats[floatInd] = toFloat;
    }
  }    

  public static void putFloats(float[] fromFloats, byte[] toBytes) {
    putFloats(fromFloats.length, fromFloats, toBytes);
  }
  public static void putFloats(int floatCount, float[] fromFloats, byte[] toBytes) {
    putFloats(floatCount, fromFloats, 0, toBytes);
  }
  public static void putFloats(float[] fromFloats, int toOffset, byte[] toBytes) {
    putFloats(fromFloats.length, fromFloats, toOffset, toBytes);
  }
  public static void putFloats(int floatCount, float[] fromFloats, int toOffset, byte[] toBytes) {
    for (int floatInd = 0; floatInd < floatCount; ++floatInd) {
      putFloat(fromFloats[floatInd], toOffset + floatInd * INT_SIZE, toBytes);
    }
  }    

  public static float getFloat(int offset, byte[] fromBytes) {
    final float toFloat;

    final int fromInt = getInt(offset, fromBytes);
    toFloat = Float.intBitsToFloat(fromInt);

    return toFloat;
  }

  public static void putFloat(float fromFloat, int offset, byte[] toBytes) {
    final int fromInt = Float.floatToRawIntBits(fromFloat);
    putInt(fromInt, offset, toBytes);
  }
  
  public static void putInts(int[] fromInts, byte[] toBytes) {
    for (int intInd = 0; intInd < fromInts.length; ++intInd) {
      putInt(fromInts[intInd], intInd * INT_SIZE, toBytes);
    }
  }
  public static void getInts(int[] toInts, byte[] fromBytes) {
    for (int intInd = 0; intInd < toInts.length; ++intInd) {
      final int fromInt = getInt(intInd * 4, fromBytes);
      int toInt = fromInt;
      toInts[intInd] = toInt;
    }
  }    
  public static void getInts(int[] toInts, byte[] fromBytes, int fromOffset) {
    for (int intInd = 0; intInd < toInts.length; ++intInd) {
      final int fromInt = getInt(fromOffset + (intInd * INT_SIZE), fromBytes);
      int toInt = fromInt;
      toInts[intInd] = toInt;
    }
  }    

  public static int getInt(int offset, byte[] fromBytes) {
    final int toInt;

    toInt = 
        ((0xff & fromBytes[offset + 0]) << 24) |
        ((0xff & fromBytes[offset + 1]) << 16) |
        ((0xff & fromBytes[offset + 2]) << 8) |
        ((0xff & fromBytes[offset + 3]) << 0);
    
    return toInt;
  }

//  public static String testPut(int put) {
//    byte[] byteArr = new byte[4];
//    int offset = 0;
//    putInt(put, offset, byteArr);
//    int get = getInt(offset, byteArr);
//    System.err.println("put " + put);
//    System.err.println("get " + get);
//    String byteStr = Arrays.toString(byteArr);
//    System.err.println("pbyte " + byteStr);
//    putInt(get, offset, byteArr);
//    System.err.println("gbyte " + Arrays.toString(byteArr));
////    ByteBuffer.allocate(0).
//    return byteStr;
//  }
  
  public static void putInt(int value, int offset, byte[] byteArr) {
    byteArr[offset + 0] = (byte)(value >>> 24);
    byteArr[offset + 1] = (byte)(value >>> 16);
    byteArr[offset + 2] = (byte)(value >>> 8);
    byteArr[offset + 3] = (byte)(value >>> 0);
  }

  public static final int INT_SIZE = 4;
  
  private static byte clampByte(float value) {
    return (byte)clamp(Byte.MIN_VALUE, Byte.MAX_VALUE, value);
  }

  private static float clamp(float min, float max, float value) {
    final float clamp;
    if (value < min) {
      clamp = min;
    } else if (value > max) {
      clamp = max;
    } else {
      clamp = value;
    }
    return clamp;
  }
  
  public static int intForByte(byte b) {
    int value = b & 0xFF;
    return value;
  }

//private static final float BYTE_SCALE = Math.max(-Byte.MIN_VALUE, Byte.MAX_VALUE);
//  private static byte clampByte(float value) {
//    return (byte)clamp(Byte.MIN_VALUE, Byte.MAX_VALUE, value);
//  }
//
//  private static float clamp(float min, float max, float value) {
//    final float clamp;
//    if (value < min) {
//      clamp = min;
//    } else if (value > max) {
//      clamp = max;
//    } else {
//      clamp = value;
//    }
//    return clamp;
//  }
}
