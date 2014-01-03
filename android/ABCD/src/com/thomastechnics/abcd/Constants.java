package com.thomastechnics.abcd;

public class Constants {
  // general timer
  public static final int MIN_TIMEOUT = 5;
  public static final int MIN_DELAY = 20;

  // ui
  public static final int PREVIEW_REPEAT = 25; //11;
  public static final int LOG_REPEAT = 1111;
  
  // camera
  public static final int PREVIEW_JPEG_QUALITY = 33;
  public static final int PREVIEW_BUFFER_COUNT = 4;
  
  // usb
  public static final int USB_REPEAT = 100;
  
  // sensor
  public static final int SENSOR_REPEAT = 100; //22;

  // pilot
  public static final float PWM_SCALE = 1.66f;
  
  // homing
  public static final int HOME_DELAY = 1000;
  public static final float HOME_EXTRA_ALT = 100;
  public static final float HOME_THROTTLE = 0.66f;

  // network
  public static final int CONNECT_REPEAT = 50;
  public static final int SOCKET_REPEAT = 22;
  public static final int DATA_REPEAT = 25; //22;
  
  public static final int LISTEN_TIMEOUT = 7000;
  public static final int CONNECT_TIMEOUT = 5000;
  public static final int READ_TIMEOUT = MIN_TIMEOUT;
  public static final int CLOSE_TIMEOUT = MIN_DELAY;

  public static final int MAX_UNREPLIED_WRITES = 2;
  public static final long MAX_WRITE_DELAY = 1000;

  public static final int MAX_READ_LENGTH = 1024;
  public static final int MAX_READ_COUNT = MAX_UNREPLIED_WRITES;
}
