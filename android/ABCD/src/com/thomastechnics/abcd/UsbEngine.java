package com.thomastechnics.abcd;

import java.io.IOException;
import java.util.Arrays;

import android.graphics.AvoidXfermode;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

public class UsbEngine implements UsbSerialTransfer.Listener {
  public byte[] readBuffer = new byte[1024];
  public int readIndex;

  public static class WriteTask implements Runnable {
    private final UsbEngine listener;
    private final String message;
    private final byte[] data;
    public WriteTask(UsbEngine listener, String message, byte[] data) {
      this.listener = listener;
      this.message = message;
      this.data = data;
    }
  
    @Override
    public void run() {
      listener.write(data);
    }
  }

  public static class AssertReadTask implements Runnable {
    private final UsbEngine listener;
    private final String message;
    private final byte[] data;
    public AssertReadTask(UsbEngine listener, String message, byte[] data) {
      this.listener = listener;
      this.message = message;
      this.data = data;
    }
  
    @Override
    public void run() {
      boolean isEqual = Arrays.equals(data, Arrays.copyOfRange(listener.readBuffer, 0, data.length));
      listener.activity.appendStatus(message + "(" + isEqual + ")" + Enummer.printBytes(data));
    }
  }

  /**
   * 
   */
  private final AbcdActivity activity;

  /**
   * @param abcdActivity
   */
  UsbEngine(AbcdActivity abcdActivity) {
    activity = abcdActivity;
  }

  private  UsbSerialTransfer.Writer writer;

  @Override
  public void onNewData(UsbSerialDriver driver, byte[] data) {
//    activity.appendStatus("serial onNewData" + Enummer.printBytes(data));
    synchronized (readBuffer) {
      final int copyLength = Math.min(readBuffer.length - readIndex, data.length);
      System.arraycopy(data, 0, readBuffer, readIndex, copyLength);
      readIndex += copyLength;
    }
  }

  public int readData(byte[] data) {
    int readCount;
    
    synchronized(readBuffer) {
      // Waits to read until there is at least one complete data.
      if (readIndex >= data.length) {
        readCount = data.length;
        readIndex -= readCount;
        byte[] readClone = readBuffer.clone();
        System.arraycopy(readClone, 0, data, 0, readCount);
        System.arraycopy(readClone, readCount, readBuffer, 0, readIndex);
      } else {
        readCount = 0;
      }
    }
    
    return readCount;
  }
  
  @Override
  public void onRunError(UsbSerialDriver driver, Exception e) {
//    activity.appendStatus("serial onRunError(" + e.getMessage() + ")" + Arrays.deepToString(e.getStackTrace()));
    AbcdActivity.appendStatus("UsbEngine onRunError " + e);
    activity.endUsb();
    try {
      driver.close();
    } catch (IOException e1) {
      AbcdActivity.appendStatus("UsbEngine onRunError close " + e1);
    }
    driver = null;
  }

  @Override
  public void onAttach(UsbSerialDriver driver, UsbSerialTransfer.Writer writer) {
    activity.appendStatus("serial onAttach");
    synchronized (writer) {
//      this.driver = driver;
      activity.appendStatus("serial device " + driver.getDevice());
      this.writer = writer;
      byte[] data = {
//          ' ', ' ', ' ', ' ', ' ', ' ', '!', 
//        ' ', ' ', ' ', ' ', ' ', ' ', '!', 
        ' ', ' ', ' ', ' ', ' ', ' ', ' ', 
          '.'};
      writer.write(data);
    }
    activity.onUsb();
  }

  @Override
  public void onDetach(UsbSerialDriver driver) {
    activity.appendStatus("serial onDetach");
    activity.endUsb();
    this.writer = null;
  }

  public void write(byte[] data) {
//    activity.appendStatus("serial write("+writer+")" + Enummer.printBytes(data));
      
    if (writer != null) {
      synchronized (writer) {
        writer.write(data);
      }
    }
  }
  
//  public boolean isAttached() {
//    boolean isAttached = (writer != null); 
//    return isAttached;
//  }
}