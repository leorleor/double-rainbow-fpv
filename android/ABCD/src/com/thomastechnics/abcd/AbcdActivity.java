package com.thomastechnics.abcd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thomastechnics.abcd.ActionEngine.Actions;
import com.thomastechnics.abcd.DataEngine.DataUpdate;
import com.thomastechnics.abcd.GpsEngine.GpsValue;

public class AbcdActivity extends Activity {

  // private final ExecutorService executor =
  // Executors.newSingleThreadExecutor();
  public final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  // Android lifecycle methods.

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    doCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
//    setBrightness(1);
    transfer.onResume();
    super.onResume();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    appendStatus("onConfigurationChanged " + newConfig);
    super.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // transfer.onPause();
  }

  @Override
  public void onBackPressed() {
    transfer.onPause();
    endCamera();
    endNetwork();
    endSensors();

    // This seems to work well. finish seemed to not stop every thing.
    System.exit(0);
    // super.onBackPressed();
    // finish();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  public void finish() {
    super.finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  // Create helper methods.

  private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
  private static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
  private static final String NEWLINE = "\n";

  private TextView statusText;
  private ScrollView statusScroll;
  private RelativeLayout contentView;
  private DataEngine dataEngine;
  private UpEngine modelUp;
  private UpEngine controlUp;
  private GpsEngine gpsEngine;
  private ActionEngine actionEngine;
  private ImageView cameraImage;
  private LinearLayout contentRow;
  
  public void doCreate(Bundle savedInstanceState) {
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//    setBrightness(1);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

    contentView = new RelativeLayout(this);
    setContentView(contentView);
    int systemUiVisibility =
    // View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
    // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    View.SYSTEM_UI_FLAG_LOW_PROFILE;
    contentView.setSystemUiVisibility(systemUiVisibility);

    RelativeLayout.LayoutParams relParams;
    // LinearLayout.LayoutParams colParams;

    contentRow = new LinearLayout(this);
    contentRow.setOrientation(LinearLayout.HORIZONTAL);
    relParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    relParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
    relParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
    relParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
    relParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
    contentRow.setHorizontalGravity(Gravity.RIGHT);
    contentRow.setLayoutParams(relParams);
    contentView.addView(contentRow);

    dataEngine = new DataEngine();
    dataEngine.add(DataId.CONTROL, controlData);
    dataEngine.add(DataId.MODEL, modelData);
    dataEngine.add(DataId.SENSOR, sensorData);
    dataEngine.add(DataId.CAMERA, cameraData);
    dataEngine.add(DataId.PREVIEW, previewData);
    dataEngine.add(DataId.ACK_CAMERA, ackData);
    dataEngine.add(DataId.ACK_PREVIEW, ackPreviewData);
//    dataEngine.add(DataId.ACCEL, new ByteData(3 * ByteUtil.INT_SIZE));
//    dataEngine.add(DataId.MAG, new ByteData(3 * ByteUtil.INT_SIZE));
    
//    dataEngine.listen(DataId.SENSOR, new SensorDataListener());
    
    dataEngine.add(DataId.MODEL_SETUP, new ByteData(DataId.SETUP_SIZE * ByteUtil.INT_SIZE));
    
    modelUp = new UpEngine();
    controlUp = new UpEngine();
    gpsEngine = new GpsEngine();
    home = gpsEngine.cloneGps();

    dataEngine.listen(DataId.CONTROL, new DataListener(){
      private boolean enableHome;

      @Override
      public void onDataChange(String id, ByteData data) {
        if (isModel) {

        int command = ByteUtil.getInt(DataId.COMMAND, data.bytes);
//        System.err.println("control command(" + command + ")");
//        if ((command >= 0) && (command < DataId.SETUP_SIZE)) {
          
          int value = ByteUtil.getInt(DataId.DATA, data.bytes);
//          int floatValue = ByteUtil.getFloat(DataId.DATA, data.bytes);
          //          int oldValue = ByteUtil.getInt(command, dataEngine.get(DataId.MODEL_SETUP).bytes);
          //          if (oldValue != value) {
          //            OffsetDataUpdate update = new OffsetDataUpdate(DataId.MODEL_SETUP, command, 0, true, -1);
          //            update.doUpdate(dataEngine, value, 0);
          //          }
          
          int time = ByteUtil.getInt(DataId.TIME, data.bytes);
          
          switch (command) {
          case DataId.SETUP_HOME_LAT:
            home[commandIntIndex(command, DataId.SETUP_HOME_LAT)] = (180 * (value / 1024f)) - 90;;
            break;
          case DataId.SETUP_HOME_LON:
            home[commandIntIndex(command, DataId.SETUP_HOME_LAT)] = (360 * (value / 1024f)) - 180;
            break;
          case DataId.SETUP_HOME_ALT:
            home[commandIntIndex(command, DataId.SETUP_HOME_LAT)] = value;
            break;
          case DataId.SETUP_HOME_THROTTLE:
            homeThrottle = value / (float)UiEngine.THROTTLE_MAX;
            break;
          case DataId.SETUP_CAM_ZOOM:
            // updateCameraZoom(value);
            camZoom[0] = value; 
            break;
          case DataId.SETUP_CAM_QUALITY:
            camQuality[0] = Math.min(100, Math.max(0, value));
            break;
          case DataId.SETUP_CAM_PIXELS:
            updateCameraPixels(value);
            break;
          case DataId.SETUP_FLAT_UP_X:
          case DataId.SETUP_FLAT_UP_Y:
          case DataId.SETUP_FLAT_UP_Z:
            flat[commandIntIndex(command, DataId.SETUP_FLAT_UP_X)] = value;
            break;
          case DataId.SETUP_TRIM_AILERON:
          case DataId.SETUP_TRIM_ELEVATOR:
          case DataId.SETUP_TRIM_THROTTLE:
          case DataId.SETUP_TRIM_RUDDER:
          case DataId.SETUP_TRIM_AUX:
            trim[commandIntIndex(command, DataId.SETUP_TRIM_AILERON)] = value;
            break;
          case DataId.COMMAND_MODE:
            if (value != DataId.MODE_NONE) {
              mode[0] = value;
            }
            break;
          case DataId.COMMAND_THROTTLE:
//            AbcdActivity.appendStatus("command_throttle " + throttle);
            throttle = value / (float)UiEngine.THROTTLE_MAX;
            break;
          case DataId.COMMAND_AUX:
            aux = value / (float)UiEngine.THROTTLE_MAX;
            break;
          case DataId.COMMAND_FLAT:
//          AbcdActivity.appendStatus("command_flat " + value);
            if (value > 0) {
              zeroModel();
            }
            break;
          case DataId.COMMAND_REC:
            if (isModel) {
              boolean rec = (value != 0);
              cameraEngine.updateRec(rec);
            }
          case DataId.COMMAND_HOME:
            // enableHome = (value > 0);
            if (value >0) {
              zeroHome();
            }
            break;
          default:
//            appendStatus("Unexpected command(" + command + ") value(" + value + ")");
          }
//        }
          
            int[] values = {
                command,
                value,
            };
            ArrayDataUpdate update = new ArrayDataUpdate(DataId.MODEL, DataId.COMMAND, values, true, DataId.TIME);
            update.doUpdate(dataEngine, System.currentTimeMillis());
          } else {
//            ArrayDataUpdate update = new ArrayDataUpdate(DataId.MODEL, DataId.COMMAND, values, true, DataId.TIME);
//            update.doUpdate(dataEngine, System.currentTimeMillis());
          }
      }
    });
    
//    for (int index = 0; index < usbReaderMap.length; ++index) {
//      final String id = usbReaderMap[index].getId();
//      if (dataEngine.get(id) == null) {
//        dataEngine.add(id, new ByteData(id, ByteUtil.FLOAT_SIZE));
//      }
//    }
    dataEngine.add("readData", new ByteData(3));
    dataEngine.add("saveData", new ByteData(3));

    actionEngine = new ActionEngine(this);

    uiEngine = new UiEngine(this, contentRow, actionEngine, dataEngine);
    uiEngine.createUi();

    LayoutParams params;

    statusScroll = new ScrollView(this);
    params = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 5);
    statusScroll.setLayoutParams(params);
    statusScroll.setScrollbarFadingEnabled(false);
    if (freezeAppend) {
      statusScroll.setVisibility(View.GONE);
    }

    statusText = new TextView(this);
    params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    statusText.setLayoutParams(params);
    statusText.setTextSize(10f);
    statusText.setTextIsSelectable(true);
    statusText.setClickable(false);
    statusScroll.addView(statusText);
    contentRow.addView(statusScroll, 0);

    transferListener = new UsbEngine(this);
    transfer.onCreate(this, transferListener);

    sensorListener = new SensorListener(this, dataEngine, DataId.SENSOR, 
//        sensorAccel,
        2.0f / SensorManager.STANDARD_GRAVITY, sensorManager);
    // gravityListener = new SensorListener(dataEngine, DataId.SENSOR,
    // sensorGravity, 2.0f / SensorManager.STANDARD_GRAVITY, sensorManager);
    loopNetwork(isModel);
    loopSensors();
//    onUsb();
  }
  
  private static int commandIntIndex(int command, int minCommand) {
    int index = (command - minCommand) / ByteUtil.INT_SIZE;
    return index;
  }

  public float[] home;
  
  public int[] flat = {
      1,0,0,
  };

  public int[] trim = {
      UiEngine.TRIM_CENTER,
      UiEngine.TRIM_CENTER,
      UiEngine.TRIM_CENTER,
      UiEngine.TRIM_CENTER,
      0,
//      0,0,0,0,0,
  };
  float[] killPwm = {
      0.2f,
      0.2f,
      -1,
      0.2f,
      -1,
  };
  
  public int[] mode = {
      0,
  };
  
  public int[] command = {
      0, 0, 0,
  };
  
  private int[] camQuality = {
    Constants.PREVIEW_JPEG_QUALITY  
  };
  
  // Logging ui.
  public boolean freezeAppend = true;

  private int[] camZoom = {
      0,
  };
//  private void updateCameraZoom(final int value) {
//    camZoom[0] = value;
//    this.runOnUiThread(new Runnable() {
//      @Override
//      public void run() {
//        if (camera != null) {
//          Camera.Parameters params = camera.getParameters();
//          if (params.isZoomSupported()) {
//            int max = params.getMaxZoom();
//            int zoom = params.getZoom();
//            int newZoom = Math.min(max, value);
//            if (newZoom != zoom) {
//              params.setZoom(zoom);
//            }
//            camera.setParameters(params);
//          }
//        }
//      }
//    });
//  }
  
  public void toggleFreezeAppend() {
    this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (freezeAppend) {
          if (statusScroll.getVisibility() != View.GONE) {
            statusScroll.setVisibility(View.GONE);
          } else {
            statusText.append(freezeText);
            freezeText = new StringBuilder();
            statusScroll.setVisibility(View.VISIBLE);
            statusScroll.fling(1000);
            freezeAppend = false;
          }
        } else {
          freezeAppend = true;
        }

        appendStatus("freeze " + freezeAppend + " " + statusScroll.getVisibility());
      }
    });
  }

  // yuk. its only used for logging, to be replaced.
  public static AbcdActivity staticLogger;
  {
    staticLogger = this;
  }

  public static void appendStatus(final String status) {
    staticLogger.doAppendStatus(status);
  }

  public StringBuilder freezeText = new StringBuilder();

  public void doAppendStatus(final String status) {
    System.err.println("ABCD " + status);

    this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (freezeAppend) {
          if (freezeText.length() > 64 * 1024) {
            freezeText = new StringBuilder("...");
          }
          freezeText.append(status + NEWLINE);
        } else {
          if (statusText.length() > 64 * 1024) {
            statusText.setText("...");
          }
          statusText.append(status + NEWLINE);
          statusScroll.fling(1000);
        }
      }
    });
  }

  private class LogTask extends TimerTask {
    @Override
    public void run() {
//      appendStatus(sensorData.toString());
//      appendStatus(controlData.toString());
//      appendStatus(modelData.toString());
//      appendStatus(dataEngine.get(DataId.VOLT_LIPO) + "");
//      appendStatus(dataEngine.get(DataId.VOLT_BEC) + "");
//      appendStatus(dataEngine.get("readData") + "");
//      appendStatus(dataEngine.get("saveData") + "");
      // appendStatus(previewData.toString());
      // appendStatus(cameraData.toString());
      
//      appendStatus("goal " + Arrays.toString(goal));
//      appendStatus("pwm " + Arrays.toString(pwm));
//      String bm = "" + previewBitmap;
//      if (previewBitmap != null) {
//        bm += " " + previewBitmap.getByteCount() + " " + previewBitmap.getWidth() + "x" + previewBitmap.getHeight();
//        appendStatus("preview " + previewData.timestamp + " " + previewData.length + " " + bm);
//      }
    }
  }

  // Usb helpers

  boolean isModel = false;
  Object modelLock = new Object();

  private final UsbSerialTransfer transfer = new UsbSerialTransfer();
  public UsbEngine transferListener;

  private Timer usbTimer;
//  float[] readUsbValues = new float[PWM_LENGTH];
  float[] pwm = new float[PWM_LENGTH];

  public void onUsb() {
    synchronized (isUsb) {
      isUsb = true;
    }
    appendStatus("onUsb " + isModel + " " + isUsb);
    synchronized (modelLock) {
      if (!isModel) {
        endNetwork();
        // endUsb();
        // endSensors();
        // endCamera();

        isModel = true;
        loopSensors();
        loopCamera();
        loopUsb();
        loopNetwork(isModel);
      }
    }
  }

  public void align() {
    if (transferListener != null) {
      byte[] data = { ' ', ' ', ' ', ' ', ' ', ' ', };
//      byte[] data = { ' ', ' ', ' ', };
      transferListener.write(data);
    }
  }

  public void restartUsb() {
    if (transferListener != null) {
      if (usbTimer != null) {
        endUsb();

        byte[] data = { ' ', ' ', ' ', ' ', ' ', ' ', '!', };
        transferListener.write(data);
      }

      transfer.onPause();
      sleepSafe(250);
      transfer.onResume();
      sleepSafe(250);
      align();
    }
  }

  private void loopUsb() {
    // endUsb();

    usbTimer = new Timer();
    usbTimer.scheduleAtFixedRate(new UsbTask(), 250, Constants.USB_REPEAT);
  }

  Boolean isUsb = false;

  public void endUsb() {
    synchronized (isUsb) {
      isUsb = false;
    }
    // if (usbTimer != null) {
    // usbTimer.cancel();
    // usbTimer = null;
    // sleepSafe(250);
    // }
    // endNetwork();
  }

//  private final String[] usbReadMap = { 
//      DataId.VOLT_LIPO, DataId.VOLT_BEC, 
//      };
//  private static final int MODEL_DATA_READ_INDEX = 3 * ByteUtil.FLOAT_SIZE;
  private final int[] gpsByteValues = new int[GPS_LENGTH];
  private ArrayDataUpdate gpsUpdate = new ArrayDataUpdate(DataId.MODEL, DataId.MODEL_LAT,
      gpsByteValues, true, DataId.TIME);
  
  private float[] goal = new float[Vec.XYZ_LENGTH];
  private int[] goalByteValues = new int[goal.length];
  private ArrayDataUpdate goalUpdate = new ArrayDataUpdate(DataId.MODEL, DataId.GOAL_UP_X,
      goalByteValues, true, DataId.TIME);

  private float[] up = new float[Vec.XYZ_LENGTH];
  private int[] upByteValues = new int[up.length];
  private ArrayDataUpdate upUpdate = new ArrayDataUpdate(DataId.MODEL, DataId.MODEL_UP_X,
      upByteValues, true, DataId.TIME);
  
  private final int[] pwmByteValues = new int[PWM_LENGTH];
  private ArrayDataUpdate pwmUpdate = new ArrayDataUpdate(DataId.MODEL, DataId.MODEL_PWM_AILERON,
      pwmByteValues, true, DataId.TIME);
  
  private final int[] voltValues = new int[VOLT_LENGTH];
  private ArrayDataUpdate voltUpdate = new ArrayDataUpdate(DataId.MODEL, DataId.MODEL_VOLT_LIPO,
      voltValues, true, DataId.TIME);
//  private final OffsetDataUpdate[] pwmReaderMap = {
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_PWM_AILERON, 0, true, DataId.TIMESTAMP),
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_PWM_ELEVON, 0, true, DataId.TIMESTAMP),
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_PWM_THROTTLE, 0, true, DataId.TIMESTAMP),
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_PWM_RUDDER, 0, true, DataId.TIMESTAMP),
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_PWM_AUX, 0, true, DataId.TIMESTAMP),
//  };
//  private final OffsetDataUpdate[] usbReaderMap = {
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_VOLT_LIPO, 0, true, DataId.TIMESTAMP),
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_VOLT_BEC, 0, true, DataId.TIMESTAMP),
//      new OffsetDataUpdate(DataId.MODEL, DataId.MODEL_VOLT_AUX, 0, true, DataId.TIMESTAMP),
//  };

//  private final ByteData usbReadBuffer = new ByteData("usbReadBuffer", ByteUtil.FLOAT_SIZE);

  private class UsbTask extends TimerTask {
    private final ByteData controlBuffer = new ByteData();
    private final ByteData sensorBuffer = new ByteData();
    private final ByteData modelBuffer = new ByteData();

    @Override
    public void run() {
      synchronized (isUsb) {
        // The model sends and then reads from usb.
        if (!isUsb) {
          transfer.onResume();
        } else {
          long time = System.currentTimeMillis();

          // read gyro last known value
          // read network last known value
          // float[] networkValues = cloneNetworkValues();
          controlData.copyTo(controlBuffer);
          sensorData.copyTo(sensorBuffer);
          modelData.copyTo(modelBuffer);
          
          float[] accel = new float[Vec.XYZ_LENGTH];
          ByteUtil.getFloats(accel, sensorBuffer.bytes);
          
          boolean kill = false;
          
          float[] gps = gpsEngine.cloneGps();
          if (mode[0] == DataId.MODE_HOME) {
            modelUp.updateHome(gps, home, goal);
            throttle = homeThrottle;
          } else if (mode[0] == DataId.MODE_ON) {
            ByteUtil.getFloats(goal, DataId.GOAL_UP_X, controlBuffer.bytes);
          } else {
            kill = true;
          }
          
          final float[] sendPwm;
          if (kill) {
            sendPwm = killPwm;
          } else {
            modelUp.updatePwm(accel, throttle, aux, goal, trim, up, pwm);
            sendPwm = pwm;
          }
          
          // appendStatus("usb values " + Arrays.toString(sensorValues));

          int sendIndex = 0;

          // send usb values
          align();

          byte[] sendBytes = new byte[(sendPwm.length + 1) * 3];
          for (sendIndex = 0; sendIndex < sendPwm.length; ++sendIndex) {
            byte indexByte = (byte) Character.forDigit(2 + sendIndex, 10);
            int value = (int) (sendPwm[sendIndex] * 128);
            if (sendIndex < pwmByteValues.length) {
              pwmByteValues[sendIndex] = Float.floatToRawIntBits(sendPwm[sendIndex]);
            }
            float scaleValue = value;
            // byte valueByte = (byte) Math.max(Byte.MIN_VALUE,
            // Math.min(Byte.MAX_VALUE, (int)scaleValue));
            byte valueByte = (byte) Math.max(0, Math.min(255, (int) (scaleValue + 128)));
            sendBytes[(sendIndex * 3) + 0] = 'S';
            sendBytes[(sendIndex * 3) + 1] = indexByte;
            sendBytes[(sendIndex * 3) + 2] = valueByte;
          }
          sendBytes[(sendPwm.length * 3) + 0] = 'A';
          sendBytes[(sendPwm.length * 3) + 1] = sendBytes[((sendPwm.length - 1) * 3) + 1];
          sendBytes[(sendPwm.length * 3) + 2] = sendBytes[((sendPwm.length - 1) * 3) + 2];

          // appendStatus("usb send " + Enummer.printBytes(sendBytes));
          transferListener.write(sendBytes);
          // appendStatus("usb sent " + Enummer.printBytes(sendBytes));

          pwmUpdate.doUpdate(dataEngine, time);

          for (int index = 0; index < up.length; index++) {
            upByteValues[index] = Float.floatToRawIntBits(up[index]);
          }
          upUpdate.doUpdate(dataEngine, time);

          for (int index = 0; index < goal.length; index++) {
            goalByteValues[index] = Float.floatToRawIntBits(goal[index]);
          }
          goalUpdate.doUpdate(dataEngine, time);

          gpsByteValues[0] = Float.floatToRawIntBits(gps[GpsValue.GPS_LAT.index] * 1000);
          gpsByteValues[1] = Float.floatToRawIntBits(gps[GpsValue.GPS_LON.index] * 1000);
          gpsByteValues[2] = Float.floatToRawIntBits(gps[GpsValue.GPS_ALT.index]);
          gpsUpdate.doUpdate(dataEngine, time);

          
          
          time = System.currentTimeMillis();
          
          // Sends usb read value requests.
          byte[] sendReadBytes = new byte[2 * voltValues.length];
          for (int readIndex = 0; readIndex < voltValues.length; ++readIndex) {
            byte indexByte = (byte) Character.forDigit(readIndex, 10);
            sendReadBytes[2 * readIndex + 0] = 'R';
            sendReadBytes[2 * readIndex + 1] = indexByte;
            
          }
          // appendStatus("usb send read" + Enummer.printBytes(sendReadBytes));
          transferListener.write(sendReadBytes);
          // appendStatus("usb sent read" + Enummer.printBytes(sendReadBytes));
          
          // Reads any usb responses.
          byte[] readBytes = new byte[3];
          while (transferListener.readData(readBytes) > 0) {
//             appendStatus("usb read " + Enummer.printBytes(readBytes));
            ByteData readData = new ByteData("read", readBytes.length);
            readData.bytes = readBytes;

            // AbcdActivity.appendStatus("read " + Arrays.toString(readBytes));
            if (readBytes[0] == 'r') {
              // Receives the value from the read response.
              // AbcdActivity.appendStatus("save " + Arrays.toString(readBytes));
              int index = readBytes[1] - ByteUtil.ASCII_ZERO;
              if (index < voltValues.length) {
                int value = ByteUtil.intForByte(readBytes[2]);
                voltValues[index] = Float.floatToRawIntBits(value * 5f / 255);
              }
            }
          }
          
         voltUpdate.doUpdate(dataEngine, time);
        }
      }
    }
  }
  
  private class VoltModelTask extends TimerTask {
    private FloatDataUpdate update = new FloatDataUpdate(DataId.MODEL, DataId.MODEL_VOLT_MODEL,  0, true);

    @Override
    public void run() {
      float percent = -1;
      int charging = 0;
      
      {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        percent = level / (float)scale;

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL;

        // How are we charging?
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
        if (isCharging) {
          charging += 1;
        }
        if (usbCharge) {
          charging += 2;
        }
        if (acCharge) {
          charging += 4;
        }
      }
      
//      AbcdActivity.appendStatus(
//          "charging " + charging + 
//          " percent " + percent +
//          " lipo " + ByteUtil.getInt(DataId.MODEL_VOLT_LIPO, dataEngine.get(DataId.MODEL).bytes) +
//          " bec " + ByteUtil.getInt(DataId.MODEL_VOLT_BEC, dataEngine.get(DataId.MODEL).bytes) +
//          " aux " + ByteUtil.getInt(DataId.MODEL_VOLT_AUX, dataEngine.get(DataId.MODEL).bytes) +
//          ""
//          );

      if (isModel) {
        update.doUpdate(dataEngine, percent);
      }
//      ByteUtil.putFloat(percent, 0, buffer.bytes);
//      buffer.timestamp = System.currentTimeMillis();
//      dataEngine.copyTo(buffer, id);
    }
  }

  // Camera helpers

  private boolean isCamera;
  private Timer loopCameraTimer;
  private Camera.PreviewCallback previewCallback;

  private void loopCamera() {
    if (!isCamera) {
      isCamera = true;
      this.runOnUiThread(new Runnable() {
        @Override
        public void run() {

          // Create an instance of Camera
          camera = CameraEngine.getCameraInstance();

          if (camera == null) {
            if (loopCameraTimer == null) {
              loopCameraTimer = new Timer();
            }
            loopCameraTimer.schedule(new TimerTask() {
              @Override
              public void run() {
                loopCamera();
              }
            }, 100);
          } else {

            previewCallback = new CameraDataCallback(cameraData, camQuality, camZoom);

            // Create our Preview view and set it as the content of our
            // activity.
            cameraEngine = new CameraEngine(AbcdActivity.this, camera, previewCallback);
            cameraEngine.setSize(sizeIndex, false);
            uiEngine.createCameraFrame(true).addView(cameraEngine, 0);
          }
        }
      });
    }
  }

  private void endCamera() {
    if (isCamera) {
      isCamera = false;
      if (cameraEngine != null) {
        cameraEngine.mCamera.stopPreview();
        cameraEngine.mCamera.release();
        cameraEngine = null;
      }
    }
  }

  public static final boolean JPEG_ENABLED = true;
  private static final int JPEG_BUFFER_LENGTH = 16 * 1024;

  /**
   * Exposes the buffer so that an array copy is not needed.
   */
  private static class ByteBufferOutputStream extends ByteArrayOutputStream {
    public byte[] setBuf(byte[] newBuf) {
      byte[] oldBuf = buf;
      buf = newBuf;
      return oldBuf;
    }

    public byte[] getBuf() {
      return buf;
    }
  }

  /**
   * Receives the preview from the camera and writes it to the network output
   * buffer.
   */
  private static final class CameraDataCallback implements Camera.PreviewCallback {
    private static final byte[] emptyHeader = VariableLengthData.createLengthBytes();

    private final ByteData cameraData;

    private Camera.Parameters cameraParam;
//    public int quality = Constants.PREVIEW_JPEG_QUALITY;
    public final int[] quality;
    public final int[] zoom;
    private Size size;
    private Rect rect = new Rect();
    private final ByteBufferOutputStream jpegStream = new ByteBufferOutputStream();
    private final ByteData jpegBuffer = new ByteData(JPEG_BUFFER_LENGTH);
    private byte[] cameraDataBytes;

    public CameraDataCallback(ByteData cameraData, int[] quality, int[] zoom) {
      this.cameraData = cameraData;
      this.quality = quality;
      this.zoom = zoom;
    }

    @Override
    public void onPreviewFrame(byte[] data, final Camera camera) {
      if (data != null) {
        long timestamp = System.currentTimeMillis();

        // Getting the camera parameters causes error logs on some phones,
        // So this avoid getting them every frame.
        // if (cameraParam == null) {
        cameraParam = camera.getParameters();
        size = cameraParam.getPreviewSize();
        // }

        if (JPEG_ENABLED) {
          jpegStream.setBuf(jpegBuffer.bytes);
          jpegStream.reset();
          try {
            jpegStream.write(emptyHeader);
          } catch (IOException e) {
//            appendStatus("jpegStream.write " + e);
            jpegStream.reset();
          }

          if (jpegStream.size() > 0) {
            YuvImage image = new YuvImage(data, cameraParam.getPreviewFormat(), size.width,
                size.height, null);
            rect.set(0, 0, image.getWidth(), image.getHeight());
            boolean compress = image.compressToJpeg(rect, quality[0],
                jpegStream);
            if (!compress) {
              jpegStream.reset();
//              appendStatus("image.compressToJpeg " + compress);
            }
          }

          if (jpegStream.size() > 0) {
//            appendStatus("jpeg swap begin");
            jpegBuffer.set(jpegStream.getBuf(), jpegStream.size(), timestamp);
            VariableLengthData.writeLength(jpegBuffer);
            jpegBuffer.swapTo(cameraData);
//            appendStatus("jpeg swap ended");
          }
        } else {
          int byteCount = CameraEngine.calcByteCount(cameraParam);
          rect.set(0, 0, size.width, size.height);
//          appendStatus("yuv byteCount " + byteCount + " " + rect.width() + " " + rect.height());
          int bytesLength = byteCount + (3 * ByteUtil.INT_SIZE);
          cameraDataBytes = ByteData.lengthenAsNeeded(cameraDataBytes, bytesLength);
          ByteUtil.putInt(bytesLength, 0 * ByteUtil.INT_SIZE, cameraDataBytes);
          ByteUtil.putInt(rect.width(), 1 * ByteUtil.INT_SIZE, cameraDataBytes);
          ByteUtil.putInt(rect.height(), 2 * ByteUtil.INT_SIZE, cameraDataBytes);
          System.arraycopy(data, 0, cameraDataBytes, 3 * ByteUtil.INT_SIZE, byteCount);
          cameraData.copy(cameraDataBytes, bytesLength, timestamp);
        }

        if (data != null) {
          camera.addCallbackBuffer(data);
        }
        
//        AbcdActivity.appendStatus("zoom" + cameraParam.isZoomSupported());
        if (cameraParam.isZoomSupported()) {
          if (cameraParam.getZoom() != zoom[0]) {
            AbcdActivity.appendStatus("zoom " + zoom[0]);
            int zoomValue = Math.min(cameraParam.getMaxZoom(), zoom[0]);
//            camera.lock();
            cameraParam.setZoom(zoomValue);
            camera.setParameters(cameraParam);
//            camera.unlock();
          }
        }
        
      } else {
        AbcdActivity.appendStatus("onPreviewFrame " + data);
      }
    }
  }
  

  private class PreviewTask extends TimerTask {
    /**
     * Reads the preview from the network input buffer and sends it to the ui.
     */
    private final byte[] decodeJpegBuffer = new byte[JPEG_BUFFER_LENGTH];
    private final ByteData previewBuffer = new ByteData("preview Buffer", 0);
    private BitmapFactory.Options options;

    @Override
    public void run() {
//       appendStatus("PreviewTask " + previewData.getLength() + " " +
//       previewData.getTimestamp() + " " + previewBuffer.getLength() + " " +
//       previewBuffer.getTimestamp() );

      // Short circuit for testing internal latency.
      // ByteData previewData = cameraData;

      if ((previewData.getTimestamp() > previewBuffer.getTimestamp())
          && (previewData.getLength() > 0)) {
//        appendStatus("PreviewTask " + previewData.getLength() + " " +
//            previewData.getTimestamp() + " " + previewBuffer.getLength() + " " +
//            previewBuffer.getTimestamp() );
        
        previewData.copyTo(previewBuffer);
        ackPreviewData.setTimestamp(previewBuffer.getTimestamp());

        if (JPEG_ENABLED) {
          int offset = VariableLengthData.HEADER_LENGTH;
          int length = previewBuffer.getLength() - offset;
          if (length > 0) {
            if (options == null) {
              options = new BitmapFactory.Options();
              options.inPreferredConfig = Bitmap.Config.RGB_565;
              options.inPreferQualityOverSpeed = false;
              options.inDither = false;
              options.inTempStorage = decodeJpegBuffer;
            }

            // options = null;
            Bitmap bitmap = BitmapFactory.decodeByteArray(previewBuffer.bytes, offset, length,
                options);
            if (bitmap != null) {
              updatePreview(bitmap);
            } else {
              appendStatus("PreviewTask " + bitmap);
            }
          }
        } else {
          final int byteCount = ByteUtil.getInt(0 * ByteUtil.INT_SIZE, previewBuffer.bytes);
          final int width = ByteUtil.getInt(1 * ByteUtil.INT_SIZE, previewBuffer.bytes);
          final int height = ByteUtil.getInt(2 * ByteUtil.INT_SIZE, previewBuffer.bytes);
//          appendStatus("updatePreviewYuv " + byteCount + " " + width + " " + height);
          if (byteCount > VariableLengthData.HEADER_LENGTH) {
            updatePreviewYuv(previewBuffer.bytes);
          }
        }
      }
    }
  }

  private byte[] yuvBytes;
  private int[] rgbDatas;

  private void updatePreviewYuv(byte[] previewBytes) {
    final int previewW = ByteUtil.getInt(1 * ByteUtil.INT_SIZE, previewBytes);
    final int previewH = ByteUtil.getInt(2 * ByteUtil.INT_SIZE, previewBytes);
    int previewL = previewW * previewH;
//    appendStatus("updatePreviewYuv begin " + previewL);

    if (previewL > 0) {
//      appendStatus("updatePreviewYuv begin " + previewBytes);
      
      int yuvOffset = 3 * ByteUtil.INT_SIZE;
      int yuvLength = previewBytes.length - yuvOffset;
      if ((yuvBytes == null) || (yuvBytes.length != yuvLength)) {
        yuvBytes = new byte[yuvLength];
      }
      System.arraycopy(previewBytes, yuvOffset, yuvBytes, 0, yuvLength);

      if ((rgbDatas == null) || (rgbDatas.length != previewL)) {
        rgbDatas = new int[previewL];
      }

      CameraEngine.decodeYUV420SP(rgbDatas, yuvBytes, previewW, previewH);
      final Bitmap bitmap = Bitmap
          .createBitmap(rgbDatas, previewW, previewH, Bitmap.Config.RGB_565);
      updatePreview(bitmap);
    }
  }

  private Runnable previewUpdate;
  private Bitmap previewBitmap;
  private Bitmap doneBitmap;

  public void updatePreview(final Bitmap bitmap) {
    previewBitmap = bitmap;
    if (previewUpdate == null) {
      previewUpdate = new PreviewUpdateTask();
      runOnUiThread(previewUpdate);
    }
  }

  private class PreviewUpdateTask implements Runnable {
    @Override
    public void run() {
//       appendStatus("updatePreview begin " + bitmap);
      if (cameraImage == null) {
        cameraImage = new ImageView(AbcdActivity.this);
        cameraImage.setScaleType(ScaleType.FIT_XY);
        uiEngine.createCameraFrame(false).addView(cameraImage, 0);
      }

      previewUpdate = null;
      if (doneBitmap != previewBitmap) {
        cameraImage.setImageBitmap(previewBitmap);
        if (doneBitmap != null) {
          doneBitmap.recycle();
        }
        doneBitmap = previewBitmap;
      }
      // appendStatus("updatePreview ended " + cameraImage);
    }
  }


  // Network helpers

  // boolean enableControl;
  // float tareSensorValues[];
  // private void onNetwork() {
  // if (!hasUsb) {
  // // The controller reads and then sends to the model.
  //
  // // read network
  //
  // // calc send network values
  // float sendValues[];
  // float receiveValues[];
  // if (enableControl) {
  // float[] sensorValues = sensorListener.cloneValues();
  //
  // if (tareSensorValues == null) {
  // tareSensorValues = sensorValues;
  // }
  // for (int index = 0; index < sensorValues.length; ++index) {
  // sensorValues[index] -= tareSensorValues[index];
  // }
  //
  // sendValues = sensorValues;
  // } else {
  // tareSensorValues = null;
  // sendValues = null;
  // }
  //
  // // send network
  // // ((Float)(sendValues[0])).
  //
  // }
  // }
  //
  // boolean hasNetwork;

  /**
   * These are the controls the model will try to connect to.
   * 
   * All the IPs are on local networks because phones on mobile networks are firewalled.
   * 
   * Tunneling the phones into my local network first with VPN worked pretty well.
   * I use an Asus router that has a builtin VPN server that works with the VPN client in Android.
   * 
   * Having the phones both SSH tunnel to ports on any reachable server should also work.
   * 
   * An option with limited range is to tether (wifi connect one phone directly to the other).
   * 
   * TODO: Read these from the server field in setup once setup data is saved
   * and restored on start. But they are hard to type and setup is cleared on
   * exit so hardcode them for now.
   */
  private enum ControlAddress {

    VPN_0("192.168.10.2", 6979),

    VPN_1("192.168.10.3", 6979),

    LAN_2("192.168.1.149", 6979),

    LAN_3("192.168.1.76", 6979),

    LAN_6("192.168.1.54", 6979),

    LAN_7("192.168.1.188", 6979),

    TETHER_0("192.168.43.66", 6979),

    TETHER_1("192.168.43.103", 6979),

    TETHER_8("192.168.43.1", 6979),

    TETHER_9("192.168.43.71", 6979),

    TEST_0("localhost", 6979),

    ;

    public final String host;
    public final int port;

    private ControlAddress(String host, int port) {
      this.host = host;
      this.port = port;
    }
  };

  private int controlListenPort = 6979;
  private Timer networkTimer = new Timer();

  private boolean isModelNetwork;
  private boolean isControlNetwork;
  private DataSession dataSession;

  public void loopNetwork(boolean isModel) {
    synchronized (this) {
      if (dataSession == null) {
        dataSession = new DataSession(this);
      }

      appendStatus("loopNetwork " + isModel);
      if (isModel) {
        if (!isModelNetwork) {
          isModelNetwork = true;
          networkTimer.schedule(new StartNetworkModel(), Constants.CONNECT_REPEAT);
        }
      } else {
        isControlNetwork = true;
        networkTimer.schedule(new StartNetworkControl(), Constants.CONNECT_REPEAT);
      }
    }
  }

  private void endConnect() {
    networkTimer.schedule(new EndConnectTask(), Constants.CONNECT_REPEAT);
  }

  private class EndConnectTask extends TimerTask {
    @Override
    public void run() {
      appendStatus("EndConnectTask begin");

      for (SocketStarter starter : socketStarterList) {
        starter.stop();
      }
      socketStarterList.clear();

      appendStatus("EndConnectTask ended");
    }
  }

  public void reconnectNetwork() {
    networkTimer.schedule(new ReConnectTask(), Constants.CONNECT_REPEAT);
  }

  private class ReConnectTask extends TimerTask {
    @Override
    public void run() {
      appendStatus("reConnect begin");
      if (isModelNetwork || isControlNetwork) {
        boolean reModelNetwork = isModelNetwork;
        boolean reControlNetwork = isControlNetwork;

        new EndConnectTask().run();
        new StopDataTask(modelDataList, controlDataList).run();

        dataSession = new DataSession(AbcdActivity.this);

        if (reModelNetwork) {
          new StartNetworkModel().run();
        }
        if (reControlNetwork) {
          new StartNetworkControl().run();
        }
      }
      appendStatus("reConnect done");
    }
  }

  public void endNetwork() {
    synchronized (this) {
      if (dataSession != null) {
        isModelNetwork = false;
        isControlNetwork = false;
        dataSession = null;
        networkTimer.schedule(new EndConnectTask(), Constants.CONNECT_REPEAT);
        networkTimer.schedule(new StopDataTask(modelDataList, controlDataList),
            Constants.CONNECT_REPEAT);
      }
    }
  }

//  private static class CompDataSpec {
//    private final String id;
//    private final int offset;
//    public CompDataSpec(String id, int offset) {
//      this.id = id;
//      this.offset = offset;
//    }
//  }
//  private CompDataSpec[] modelSpec = {
//      new CompDataSpec(DataId.ACCEL, 6 * ByteUtil.FLOAT_SIZE),
////      new CompDataSpec(DataId.MODEL, 9 * ByteUtil.FLOAT_BYTE_COUNT),
//      new CompDataSpec(DataId.VOLT_BEC, 0 * ByteUtil.FLOAT_SIZE),
//      new CompDataSpec(DataId.VOLT_LIPO, 3 * ByteUtil.FLOAT_SIZE),
//      new CompDataSpec(DataId.VOLT_SONAR, 3 * ByteUtil.FLOAT_SIZE),
//      new CompDataSpec(DataId.VOLT_MODEL, 3 * ByteUtil.FLOAT_SIZE),
//  };
//
//  private String[] modelDataIds = {
//      DataId.GPS,
//      DataId.UP_MODEL,
//      DataId.UP_GOAL,
//      DataId.PWM,
//      DataId.VOLT_BEC,
//      DataId.VOLT_LIPO,
//      DataId.VOLT_MODEL,
//      DataId.VOLT_SONAR,
//  };
  
  private List<DataSocket> modelDataList;
  private List<DataSocket> controlDataList;

  private static final int PWM_LENGTH = 5;
  private static final int GPS_LENGTH = 3;
  private static final int VOLT_LENGTH = 3;
  //private static final int CONTROL_DATA_LENGTH = 5;
  //private static final int CONTROL_DATA_BYTES = CONTROL_DATA_LENGTH * ByteUtil.FLOAT_SIZE;
  private static final int CONTROL_DATA_BYTES = DataId.CONTROL_SIZE;
  //private static final int MODEL_DATA_LENGTH = 19;
  //private static final int MODEL_DATA_BYTES = MODEL_DATA_LENGTH * ByteUtil.FLOAT_SIZE;
  private static final int MODEL_DATA_BYTES = DataId.MODEL_SIZE;

  private ByteData controlData = new ByteData("control", CONTROL_DATA_BYTES);
  private ByteData modelData = new ByteData("model", MODEL_DATA_BYTES);
  private ByteData previewData = VariableLengthData.createVariable("preview", true);
  private ByteData cameraData = VariableLengthData.createVariable("camera", false);

  private class StartNetworkModel extends TimerTask {
    @Override
    public void run() {
      appendStatus("StartNetworkModel begin");

      DataConnectListener listener = new ModelSocketListener(DataId.CONTROL, DataId.MODEL,
          DataId.CAMERA, DataId.ACK_CAMERA, DataId.CONTROL_SETUP, DataId.MODEL_SETUP, dataSession);
      for (ControlAddress url : ControlAddress.values()) {
        SocketStarter socketUtil = new SocketStarter();
        listener.setStarter(socketUtil);
        socketUtil.setListener(listener);
        socketStarterList.add(socketUtil);
        socketUtil.start(false, url.host, url.port);
      }

      appendStatus("StartNetworkModel ended");
    }
  }

  private class StartNetworkControl extends TimerTask {
    @Override
    public void run() {
      appendStatus("StartNetworkControl begin");

      DataConnectListener listener = new ControlSocketListener(DataId.MODEL, DataId.CONTROL,
          DataId.PREVIEW, DataId.ACK_PREVIEW, DataId.CONTROL_SETUP, DataId.MODEL_SETUP, dataSession);
      SocketStarter socketStarter = new SocketStarter();
      listener.setStarter(socketStarter);
      socketStarter.setListener(listener);
      socketStarterList.add(socketStarter);
      socketStarter.start(true, null, controlListenPort);

      // Loops to send data if connected.
      // controlNetworkTimer.scheduleAtFixedRate(new ControlNetworkTask(), 350,
      // RATE_READ);

      appendStatus("StartNetworkControl ended");
    }
  }

  private static final int DATA_COUNT = 2;
  private ByteData ackPreviewData = new ByteData("ackPreview", 1);
  private ByteData ackData = new ByteData("ackCamera", 1);

  private List<SocketStarter> socketStarterList = new LinkedList<SocketStarter>();

  private class ControlSocketListener extends DataConnectListener {
    private final List<DataSocket> dataList;

    public ControlSocketListener(String modelId, String sensorId, String previewId,
        String ackPreviewId, String controlSetupId, String modelSetupId, DataSession session) {
      dataList = new ArrayList<DataSocket>(DATA_COUNT);
      dataEngine.get(previewId).setLength(0);
      addData(true, dataList, dataEngine, modelId, sensorId, session);
      addData(true, dataList, dataEngine, previewId, ackPreviewId, session);
//      addData(true, dataList, dataEngine, modelSetupId, controlSetupId, session);
      setData(dataList);
    }

    public void onCount() {
      appendStatus("ControlSocketListener onCount");
      endConnect();
      loopSensors();
      controlDataList = dataList;
      networkTimer.schedule(new StartDataTask(dataList), Constants.CONNECT_REPEAT);
      
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          uiEngine.setViewVisible(Actions.ACTION_THROTTLE, true);
          uiEngine.setViewVisible(Actions.ACTION_CAMERA, true);
          
//          setBrightness(1);
          }
      });
    }
  }

  private class ModelSocketListener extends DataConnectListener {
    private final List<DataSocket> dataList;

    public ModelSocketListener(String controlId, String sensorId, String cameraId,
        String ackCameraId, String controlSetupId, String modelSetupId, DataSession session) {
      dataList = new ArrayList<DataSocket>(DATA_COUNT);
//      dataEngine.get(cameraId).setLength(0);
      addData(false, dataList, dataEngine, controlId, sensorId, session);
      addData(false, dataList, dataEngine, ackCameraId, cameraId, session);
//      addData(false, dataList, dataEngine, controlSetupId, modelSetupId, session);
      setData(dataList);
    }

    public void onCount() {
      appendStatus("ModelSocketListener onCount");
      endConnect();
      modelDataList = dataList;
      networkTimer.schedule(new StartDataTask(dataList), 2 * Constants.CONNECT_REPEAT);
      
      contentView.postDelayed(
//      runOnUiThread(
          new Runnable() {
        @Override
        public void run() {
          uiEngine.setViewVisible(Actions.ACTION_HUD, false);
          uiEngine.setViewVisible(Actions.ACTION_PREVIEW, false);
          uiEngine.setViewVisible(Actions.ACTION_SETUP, false);
          uiEngine.setViewVisible(Actions.ACTION_DISPLAY, false);
          
//          PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//          WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "GLGame");
//          wakeLock.acquire();
//          pm.goToSleep(SystemClock.uptimeMillis());
          //          setBrightness(0);
//          setBrightness(0.01f);
        }
      }
          , 1000
          );
    }
  }

  private void setBrightness(float brightness) {
//    if (brightness <= 0.01f) {
//      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//      //  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//      //  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
//      //  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//      //  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
//      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//
//    } else {
//      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//      getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
////      PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
////      PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "connected");
////      wl.acquire();
//    }

    WindowManager.LayoutParams params = getWindow().getAttributes();
    params.screenBrightness = brightness;
    getWindow().setAttributes(params);
  }
  
  private boolean isData;

  private class StartDataTask extends TimerTask {
    private final List<DataSocket> dataList;

    public StartDataTask(List<DataSocket> dataList) {
      this.dataList = dataList;
    }

    @Override
    public void run() {
      appendStatus("StartDataTask begin");

      for (DataSocket data : dataList) {
        data.start();
      }
      isData = true;

      appendStatus("StartDataTask begin");
    }
  }

  private class HomeTask extends TimerTask {
    @Override
    public void run() {
      appendStatus("HomeTask");
      if (mode[0] == DataId.MODE_ON) {
        long controlTime = dataEngine.get(DataId.CONTROL).getTimestamp() ;
        long controlDelay = System.currentTimeMillis() - controlTime; 
        if (controlDelay > Constants.HOME_DELAY) {
          mode[0] = DataId.MODE_HOME;
          
          int[] values = {
              DataId.COMMAND_MODE,
              mode[0],
          };
          ArrayDataUpdate update = new ArrayDataUpdate(DataId.MODEL, DataId.COMMAND, values, true, DataId.TIME);
          update.doUpdate(dataEngine, System.currentTimeMillis());
        }
      }
    }
  }

  private class StopDataTask extends TimerTask {
    private final List<DataSocket> modelDataList;
    private final List<DataSocket> controlDataList;

    public StopDataTask(List<DataSocket> modelDataList, List<DataSocket> controlDataList) {
      this.modelDataList = modelDataList;
      this.controlDataList = controlDataList;
    }

    @Override
    public void run() {
      appendStatus("StopDataTask begin");
      if (isData) {
        stopData(modelDataList);
        stopData(controlDataList);
        isData = false;
      }
      appendStatus("StopDataTask ended");
    }

    private void stopData(List<DataSocket> dataList) {
      if (dataList != null) {
        for (DataSocket data : dataList) {
          data.stop();

          try {
            data.getSocket().close();
          } catch (IOException e) {
          }
        }
      }
    }
  }

  // Sensor helpers

  private SensorManager sensorManager;
  private Sensor sensorGrav;
  private Sensor sensorAccel;

  private SensorListener sensorListener;
  protected Camera camera;
  protected CameraEngine cameraEngine;
  private ByteData sensorData = new ByteData("sensor", SensorListener.DATA_BYTES);

  private void loopSensors() {
    appendStatus("loopSensors");

    if (isModel) {
      gpsEngine.start(this);
    }
    
    if (sensorManager == null) {
      dataEngine.add(DataId.VOLT_MODEL, new ByteData(1));

      // start gyro listener
      sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
       sensorGrav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
      sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//      sensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

      // if (SensorManager.getRotationMatrix(m_rotationMatrix, null,
      // m_lastAccels, m_lastMagFields)) {
      //
      // }
      // sensorManager.getRotationMatrix();
      // getOrientation();

//      sensorListener.setSensor(sensorAccel);
      // gravityListener.setSensor(sensorGravity);
//      sensorManager.registerListener(sensorListener, sensorAccel, SensorManager.SENSOR_DELAY_GAME);
//      sensorManager.registerListener(sensorListener, sensorMag, SensorManager.SENSOR_DELAY_GAME);
      // sensorManager.registerListener(gravityListener, sensorGravity,
      // SensorManager.SENSOR_DELAY_GAME);
      networkTimer.scheduleAtFixedRate(new VoltModelTask(), 250, Constants.LOG_REPEAT);
      networkTimer.scheduleAtFixedRate(new LogTask(), 250, Constants.LOG_REPEAT);
      appendStatus("loopPreview begin");
      networkTimer.scheduleAtFixedRate(new PreviewTask(), 1000, Constants.PREVIEW_REPEAT);
      appendStatus("loopPreview ended");
    }
    
    if (isModel) {
      sensorManager.unregisterListener(sensorListener);
      sensorManager.registerListener(sensorListener, sensorGrav, SensorManager.SENSOR_DELAY_GAME);
    } else {
      sensorManager.unregisterListener(sensorListener);
      sensorManager.registerListener(sensorListener, sensorAccel, SensorManager.SENSOR_DELAY_GAME);
    }
  }

  private void endSensors() {
    appendStatus("endSensors");

    // cancel gyro listener
    if (sensorManager != null) {
      gpsEngine.stop();
      sensorManager.unregisterListener(sensorListener);
      sensorListener = null;
      sensorAccel = null;
      sensorManager = null;
    }
  }

//  private Sensor sensorMag;

  public boolean isControl() {
    return !isModel;
  }
  public void updateGoal() {
    if (steer) {
      ByteData sensorBuffer = new ByteData();
      sensorData.copyTo(sensorBuffer);
      
      float[] accel = new float[Vec.XYZ_LENGTH];
      ByteUtil.getFloats(accel, sensorBuffer.bytes);
      
      final float[] goal = new float[Vec.XYZ_LENGTH];
      controlUp.updateGoal(accel, goal);
      
      dataEngine.update(DataId.CONTROL, new DataUpdate() {
        @Override
        public void update(String id, ByteData data) {
          ByteUtil.putFloats(goal, DataId.GOAL_UP_X, data.bytes);
          data.timestamp = Math.max(data.timestamp, sensorData.timestamp);
        }
      });
    }
  }
  // private SensorListener gravityListener;

  private static class SensorListener implements SensorEventListener {
    float[] accel;
//    float[] mag;

    private static final int DATA_LENGTH = 4;
    private static final int THROTTLE_INDEX = (DATA_LENGTH - 1);
    public static final int DATA_BYTES = DATA_LENGTH * ByteUtil.INT_SIZE;

//    private Sensor sensor;
//    private SensorManager sensorManager;
    private final float scale;
    private final String id;
    // private final ByteData sensorData;
    private float[] values = new float[DATA_LENGTH];
    // private byte[] buffer = new byte[DATA_BYTES];
    private final DataEngine dataEngine;
    private final AbcdActivity activity;
    
    public SensorListener(AbcdActivity activity, DataEngine dataEngine, String id, 
//        Sensor sensor, 
        float scale,
        SensorManager sensorManager) {
      this.activity = activity;
      this.dataEngine = dataEngine;
//      this.sensor = sensor;
//      this.sensorManager = sensorManager;
      this.scale = scale;
      this.id = id;
      // this.sensorData = sensorData;
      dataEngine.get(id).setLength(DATA_BYTES);
      // sensorData.setLength(DATA_BYTES);
    }

//    public Sensor getSensor() {
//      return sensor;
//    }

    private boolean zeroed;

//    public void zero() {
//      zeroed = false;
//    }

//    public void setSensor(Sensor sensor) {
//      this.sensor = sensor;
//    }

//    public float[] cloneValues() {
//      float[] cloneValues;
//      synchronized (this) {
//        cloneValues = values.clone();
//      }
//      return cloneValues;
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//      if (event.sensor == sensor) {
        dataEngine.update(id, new SensorUpdate(event, scale));
        
        if (activity.isControl()) {
          activity.updateGoal();
        }
//        accel = event.values.clone();
//        updateData(event.timestamp);

        // } else {
        // dataEngine.update(DataId.MAG, new SensorUpdate(event, scale));
        // mag = event.values.clone();
//      }
      // dataEngine.update(id, new SensorUpdate(event, scale));

      // if (accel != null && mag != null) {
      // }
    }

//    private final static float[] deviceX = { 1, 0, 0 };
//    private final static float[] deviceY = { 0, 1, 0 };
    // private final static float[] deviceZ = {0, 0, 1};

    // private final float[] rotation = new float[9];
    // private final float[] orientation = new float[3];
    // private final float[] inclination = new float[9];
    // private final float[] attitude = new float[3];

//    private float[] zeroX;
//    private float[] zeroY;
//    private float[] zeroZ;

//    private void updateData(long timestamp) {
//      // if (SensorManager.getRotationMatrix(rotation, inclination, accel, mag))
//      // {
//      // SensorManager.getOrientation(rotation, orientation);
//      // float ig = (float)(1 / SensorManager.GRAVITY_EARTH);
//      // float im = (float)(1 / Math.PI);
//      // }
//
//      final float[] accelNorm;
//      if (Vec.length(accel) > 0) {
//        accelNorm = Vec.normalize(accel.clone());
//      } else {
//        accelNorm = new float[] { 0, 0, 0 };
//      }
//
//      if (!zeroed) {
//        zeroX = accelNorm.clone();
//        zeroed = true;
//
////        float dZeroDotX = Vec.computeDotProduct(zeroX, deviceX);
//        float dZeroDotY = Vec.computeDotProduct(zeroX, deviceY);
////        if (Math.abs(dZeroDotY) < Math.abs(dZeroDotX)) {
//        if (Math.abs(dZeroDotY) < Math.pow(2, -0.5)) {
//          zeroZ = Vec.computeCrossProduct(zeroX, deviceY);
//        } else {
//          zeroZ = Vec.computeCrossProduct(zeroX, deviceX);
//        }
//        Vec.normalize(zeroZ);
//
//        zeroY = Vec.normalize(Vec.computeCrossProduct(zeroX, zeroZ));
//      }
//
//      float dXd = (float) Vec.computeDotProduct(accelNorm, zeroX);
//      float dYd = (float) Vec.computeDotProduct(accelNorm, zeroY);
//      float dZd = (float) Vec.computeDotProduct(accelNorm, zeroZ);
//
//      float[] zeroAccel = { dXd, dYd, dZd };
//
//      dataEngine.update(DataId.SENSOR, new SensorUpdate(zeroAccel, timestamp, 1));
//    }
//
    private static class SensorUpdate implements DataEngine.DataUpdate {
      private float[] values;
      private long timestamp;
      private final float scale;

      public SensorUpdate(SensorEvent event, float scale) {
        this(event.values, event.timestamp, scale);
      }

      public SensorUpdate(float values[], long timestamp, float scale) {
        this.values = values;
        this.timestamp = timestamp;
        this.scale = scale;
      }

      @Override
      public void update(String id, ByteData data) {
        byte[] buffer = data.bytes.clone();
        for (int index = 0; index < THROTTLE_INDEX; ++index) {
          float value = scale * values[index];
          ByteUtil.putFloat(value, index * ByteUtil.INT_SIZE, buffer);
        }
        data.set(buffer, DATA_BYTES, timestamp);
      }
    }

//    private void onThrottleChanged(float throttle) {
      // long timestamp = System.currentTimeMillis();
      // synchronized (this) {
      // values[THROTTLE_INDEX] = throttle;
      // updateData(timestamp);
      // }
//    }

    // private void updateData(long timestamp) {
    // ByteUtil.putFloats(values, buffer);
    // buffer = sensorData.set(buffer, DATA_BYTES, timestamp);
    // }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
  }

  private void sleepSafe(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private float throttle;
  private float homeThrottle = Constants.HOME_THROTTLE;
  private float aux;
  private int sizeIndex = 2;

  public void updateCameraPixels(int sizeIndex) {
    this.sizeIndex = sizeIndex;
    if (isCamera) {
      cameraEngine.setSize(sizeIndex, true);
    }
  }

  public void setThrottle(float throttle) {
    AbcdActivity.appendStatus("setThrottle " + throttle);
//    final float updateThrottle = Math.max(0, Math.min(1, throttle));
//    this.throttle = updateThrottle;
//    
////    sensorListener.onThrottleChanged(throttle);
//    
//    dataEngine.update(DataId.CONTROL, new DataUpdate() {
//      @Override
//      public void update(String id, ByteData data) {
//        ByteUtil.putFloat(updateThrottle, DataId.DATA, data.bytes);
//        data.timestamp = Math.max(data.timestamp, System.currentTimeMillis());
//      }
//    });
  }

  public float getThrottle() {
    return throttle;
  }

  private UiEngine uiEngine;

  public UiEngine getUi() {
    return uiEngine;
  }

  public void zeroModel() {
    modelUp.zero();
  }
  public void zeroControl() {
    controlUp.zero();
  }
  public void zeroHome() {
    home = gpsEngine.cloneGps();
  }
  private boolean steer;
  public void setSteer(boolean steer) {
    this.steer = steer;
  }
}
