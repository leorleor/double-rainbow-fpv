package com.thomastechnics.abcd;


public class DataId {
  public static final String PREVIEW_QUALITY = "PREVIEW_QUALITY";
  public static final String PREVIEW_RESOLUTION = "PREVIEW_RESOLUTION";
  public static final String SENSOR = "SENSOR";
  public static final String PREVIEW = "PREVIEW";
  public static final String CAMERA = "CAMERA";
  public static final String MODEL = "MODEL";
  public static final String CONTROL = "CONTROL";
  public static final String ACK_PREVIEW = "ACK_PREVIEW";
  public static final String ACK_CAMERA = "ACK_CAMERA";
  public static final String MODEL_SETUP = "MODEL_SETUP";
  public static final String CONTROL_SETUP = "CONTROL_SETUP";
  public static final String UI_SETUP = "UI_SETUP";
  
  public static final String STOP = "STOP";
  public static final String KILL = "KILL";
  
  public static final String ACCEL = "ACCEL";
  public static final String MAG = "GRAVITY";
  
  public static final String VOLT_LIPO = "VOLT_LIPO";
  public static final String VOLT_BEC = "VOLT_BEC";
  public static final String VOLT_AUX = "VOLT_AUX";
  public static final String VOLT_MODEL = "VOLT_MODEL";
  public static final String GPS = "GPS";
  public static final String UP_MODEL = "UP_MODEL";
  public static final String UP_GOAL = "UP_GOAL";
  public static final String PWM = "PWM";
  public static final String STEER = "STEER";

  
  public static final int COMMAND_MODE = 0 * ByteUtil.INT_SIZE;
  public static final int MODE_MIN = 0;
  public static final int MODE_NONE = 0 + MODE_MIN;
  public static final int MODE_KILL = 1 + MODE_MIN;
  public static final int MODE_HOME = 2 + MODE_MIN;
  public static final int MODE_ON = 3 + MODE_MIN;
//  public static final int MODE_CONTROL = 4 + MODE_MIN;

  public static final int SETUP_MIN = 1 * ByteUtil.INT_SIZE;
  public static final int SETUP_HOME_LAT = 1 * ByteUtil.INT_SIZE;
  public static final int SETUP_HOME_LON = 2 * ByteUtil.INT_SIZE;
  public static final int SETUP_HOME_ALT = 3 * ByteUtil.INT_SIZE;
  public static final int SETUP_HOME_THROTTLE = 4 * ByteUtil.INT_SIZE;
  public static final int SETUP_CAM_ZOOM = 5 * ByteUtil.INT_SIZE;
  public static final int SETUP_CAM_QUALITY = 6 * ByteUtil.INT_SIZE;
  public static final int SETUP_CAM_PIXELS = 7 * ByteUtil.INT_SIZE;
  public static final int SETUP_FLAT_UP_X = 8 * ByteUtil.INT_SIZE;
  public static final int SETUP_FLAT_UP_Y = 9 * ByteUtil.INT_SIZE;
  public static final int SETUP_FLAT_UP_Z = 10 * ByteUtil.INT_SIZE;
  public static final int SETUP_TRIM_AILERON = 11 * ByteUtil.INT_SIZE;
  public static final int SETUP_TRIM_ELEVATOR = 12 * ByteUtil.INT_SIZE;
  public static final int SETUP_TRIM_THROTTLE = 13 * ByteUtil.INT_SIZE;
  public static final int SETUP_TRIM_RUDDER = 14 * ByteUtil.INT_SIZE;
  public static final int SETUP_TRIM_AUX = 15 * ByteUtil.INT_SIZE;
  public static final int SETUP_SIZE = 16 * ByteUtil.INT_SIZE;
  
  public static final int COMMAND_MIN = SETUP_SIZE;
  public static final int COMMAND_ON = 0 + COMMAND_MIN;
  public static final int COMMAND_GO = 1 + COMMAND_MIN;
  public static final int COMMAND_CONTROL = 2 + COMMAND_MIN;
  public static final int COMMAND_THROTTLE = 3 + COMMAND_MIN;
  public static final int COMMAND_HOME = 4 + COMMAND_MIN;
  public static final int COMMAND_AUX = 5 + COMMAND_MIN;
  public static final int COMMAND_FLAT = 6 + COMMAND_MIN;
  public static final int COMMAND_REC = 7 + COMMAND_MIN;
  public static final int COMMAND_MAX =  8 + COMMAND_MIN;
  
  public static final int TIME = 0 * ByteUtil.INT_SIZE;
  public static final int COMMAND = 1 * ByteUtil.INT_SIZE;
  public static final int DATA = 2 * ByteUtil.INT_SIZE;
  
  public static final int GOAL_UP_X = 3 * ByteUtil.INT_SIZE;
  public static final int GOAL_UP_Y = 4 * ByteUtil.INT_SIZE;
  public static final int GOAL_UP_Z = 5 * ByteUtil.INT_SIZE;
  
  public static final int CONTROL_SIZE = 6 * ByteUtil.INT_SIZE;

  public static final int MODEL_UP_X = 6 * ByteUtil.INT_SIZE;
  public static final int MODEL_UP_Y = 7 * ByteUtil.INT_SIZE;
  public static final int MODEL_UP_Z = 8 * ByteUtil.INT_SIZE;
  public static final int MODEL_LAT = 9 * ByteUtil.INT_SIZE;
  public static final int MODEL_LON = 10 * ByteUtil.INT_SIZE;
  public static final int MODEL_ALT = 11 * ByteUtil.INT_SIZE;
  public static final int MODEL_VOLT_LIPO = 12 * ByteUtil.INT_SIZE;
  public static final int MODEL_VOLT_BEC = 13 * ByteUtil.INT_SIZE;
  public static final int MODEL_VOLT_AUX = 14 * ByteUtil.INT_SIZE;
  public static final int MODEL_VOLT_MODEL = 15 * ByteUtil.INT_SIZE;
  public static final int MODEL_PWM_AILERON = 16 * ByteUtil.INT_SIZE;
  public static final int MODEL_PWM_ELEVON = 17 * ByteUtil.INT_SIZE;
  public static final int MODEL_PWM_THROTTLE = 18 * ByteUtil.INT_SIZE;
  public static final int MODEL_PWM_RUDDER = 19 * ByteUtil.INT_SIZE;
  public static final int MODEL_PWM_AUX = 20 * ByteUtil.INT_SIZE;
  public static final int MODEL_SIZE = 21 * ByteUtil.INT_SIZE;
}
