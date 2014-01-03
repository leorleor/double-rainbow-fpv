package com.thomastechnics.abcd;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class UsbSerialTransfer {
  
  
  
  // Public interfaces.
  
  public interface Writer {
    public void write(byte[] data);
  }

  public interface Listener {
    public void onNewData(UsbSerialDriver driver, byte[] data);
    public void onRunError(UsbSerialDriver driver, Exception e);
    public void onAttach(UsbSerialDriver driver, Writer writer);
    public void onDetach(UsbSerialDriver driver);
  }

  
  
  // Class fields.
  
  private final String TAG = this.getClass().getName(); 

  private Context context;
  private Listener listener;
  
  /**
   * Singleton android system manager is gotten from context.
   */
  private UsbManager usbManager;

  /**
   * Serial driver for the usb device, or null if not attached.
   */
  private UsbSerialDriver driver;

  private SerialInputOutputManager serialManager;
  private DeviceBridge serialListener;

  private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

  
  
  public UsbSerialTransfer(
      ) {
  }

  
  
  // Lifecycle methods.
  
  public void onCreate(
      Context context,
      Listener listener
      ) {
    this.context = context;
    this.listener = listener;
    usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
  }

  public void onPause() {
    stopIoManager();
    if (driver != null) {
//      try {
//        driver.close();
//      } catch (IOException e) {
//        // Ignore.
//      }
      driver = null;
    }
  }

  public void onResume() {
    if (driver == null) {
      driver = UsbSerialProber.acquire(usbManager);
      //    Log.d(TAG, "Resumed, mSerialDevice=" + driver);
      if (driver == null) {
        // No serial device.
      } else {
        try {
          driver.open();
          onDeviceStateChange();
        } catch (IOException e) {
          //        Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
          try {
            driver.close();
            onDeviceStateChange();
          } catch (IOException e2) {
            // Ignore.
          }
          driver = null;
          return;
        }
        // Cans has a device.
      }
    }
  }

  
  
  // Helper methods.
  
  private void stopIoManager() {
    if (serialManager != null) {
      Log.i(TAG, "Stopping io manager ..");
      serialManager.stop();
      serialManager = null;
      serialListener = null;

      if (driver != null) {
        listener.onDetach(driver);
      }
    }
  }

  private void startIoManager() {
    if (driver != null) {
      Log.i(TAG, "Starting io manager ..");

      serialListener = new DeviceBridge(driver, listener);
      serialManager = new SerialInputOutputManager(driver, serialListener);
      mExecutor.submit(serialManager);
      listener.onAttach(driver, serialListener);
    }
  }

  private void onDeviceStateChange() {
    stopIoManager();
    startIoManager();
  }

  
  
  // Helper classes.
  
  private class DeviceBridge implements SerialInputOutputManager.Listener, Writer {
    private final UsbSerialDriver driver;
    private final Listener listener;

    public DeviceBridge(UsbSerialDriver driver, Listener listener) {
      this.driver = driver;
      this.listener = listener;
    }

    @Override
    public void onRunError(Exception e) {
      listener.onRunError(driver, e);
      UsbSerialTransfer.this.driver = null;
    }

    @Override
    public void onNewData(final byte[] data) {
      listener.onNewData(driver, data);
    }

    @Override
    public void write(byte[] data) {
      if (driver != null) {
        try {
          driver.write(data, 200);
        } catch (IOException e) {
          listener.onRunError(driver, e);
        }
      }
    }
  }
  
  
  
  // Old code.
  
//private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

//private UsbDevice usbDevice;
//private UsbManager usbManager;

//private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
//
//  public void onReceive(Context context, Intent intent) {
//    appendStatus("usbReceiver.onReceive()");
//
//    String action = intent.getAction();
//    if (ACTION_USB_PERMISSION.equals(action)) {
//      appendStatus("ACTION_USB_PERMISSION");
//
//      synchronized (this) {
//        appendStatus("synchronized");
//        UsbDevice device = (UsbDevice) intent
//            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
//
//        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//          appendStatus("EXTRA_PERMISSION_GRANTED");
//
//          if (device != null) {
//            appendStatus("device(" + device + ")");
//
//            if (device == usbDevice) {
//              appendStatus("usbDevice");
//            }
//
//            onUsbConnectPermission(device);
//          }
//        } else {
//          appendStatus("permission denied for device " + device);
//        }
//      }
//    }
//  }
//};
  
//private final class UsbTransferTask extends TimerTask {
//private final byte[] bytes = new byte[9];
//
//private UsbTransferTask() {
//}
//
//@Override
//public void run() {
//  int byteIndex = 0;
//  bytes[byteIndex++] = ' ';
//  bytes[byteIndex++] = ' ';
//  bytes[byteIndex++] = ' ';
//  bytes[byteIndex++] = ' ';
//  bytes[byteIndex++] = ' ';
//  bytes[byteIndex++] = ' ';
//  bytes[byteIndex++] = 'S';
//  bytes[byteIndex++] = '2';
//  bytes[byteIndex++] = (byte) ('0' + ((System.currentTimeMillis() / 1000) % 100));
//
//  int count;
//
//  appendStatus("out bytes(" + Enummer.printBytes(bytes));
//  count = usbConnection.bulkTransfer(outUsb, bytes, bytes.length, 100);
//  appendStatus("out bytes(" + count + ")" + Enummer.printBytes(bytes));
//
//  count = usbConnection.bulkTransfer(inUsb, bytes, bytes.length, 100);
//  appendStatus("in bytes(" + count + ")" + Enummer.printBytes(bytes));
//}
//}

}
