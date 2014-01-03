package com.thomastechnics.abcd;

import com.thomastechnics.abcd.GpsEngine.GpsValue;


public class UpEngine {
  private final static float[] deviceX = { 1, 0, 0 };
  private final static float[] deviceY = { 0, 1, 0 };

//  private final DataEngine dataEngine;
  private boolean zeroed;
  private float[] zeroX;
  private float[] zeroY;
  private float[] zeroZ;

  public UpEngine(
//      DataEngine dataEngine
      ) {
//    this.dataEngine = dataEngine;
  }
  
  public void updateGoal(float[] accel, float[] goal) {
    final float[] accelNorm = Vec.normalize(accel.clone());

    // zero as needed
    doZero(accelNorm);

    // calculate sensor in flat ufl space
    map(accelNorm, zeroX, zeroY, zeroZ, goal);
  }

  private static final float Pi2 = (float)(2 * Math.PI);
  
  // Establishes a goal based on home and gps to support RTL.
  public boolean updateHome(float[] gps, float[] home, float[] goal) {
    boolean valid = 
        (gps[GpsValue.GPS_VALID.index] > 0) &&
        (home[GpsValue.GPS_VALID.index] > 0); 

    if (valid) {
      float[] diff = new float[GpsValue.values().length];
      for (int index = 0; index < diff.length; ++index) {
        diff[index] = home[index] - gps[index];
      }

      float gpsBearing = gps[GpsValue.GPS_BEAR.index] * Pi2 / 360;
      float homeBearing = (float)(Math.PI + Math.atan2(-diff[GpsValue.GPS_LON.index], -diff[GpsValue.GPS_LAT.index]));
      float diffBearing = homeBearing - gpsBearing;
      if (diffBearing > Math.PI) {
        diffBearing = Pi2 - diffBearing;
      } else if (diffBearing < -Math.PI) {
        diffBearing = Pi2 + diffBearing;
      }

      float diffAlt = diff[GpsValue.GPS_ALT.index] + Constants.HOME_EXTRA_ALT;
      
      final float maxAngle = (float)(Math.PI * 0.33);
      final float pitchRadianPerMeter = -maxAngle / 33;
      float pitch = Math.max(-maxAngle, Math.min(maxAngle, diffAlt * pitchRadianPerMeter));
      float roll = Math.max(-maxAngle, Math.min(maxAngle, diffBearing));

      goal[0] = (float)Math.sin(roll);
      goal[1] = (float)Math.sin(maxAngle);
      goal[2] = (float)Math.sin(pitch);
      Vec.normalize(goal);
    }
    
    return valid;
  }
  
  public void updatePwm(float[] accel, float throttle, float aux, float[] goal, int[] trim, float[] up, float[] pwm) {
    final float[] accelNorm = Vec.normalize(accel.clone());

    // zero as needed
    doZero(accelNorm);
    
    // calculate sensor in flat ufl space
    // float[] up = new float[accelNorm.length];
    map(accelNorm, zeroX, zeroY, zeroZ, up);
    
    // calculate rotates from ufl sensor to goal
    float angle = (float)Math.acos(Vec.computeDotProduct(up, goal));
    float[] axis = Vec.normalize(Vec.computeCrossProduct(up, goal));
    float pitch = axis[1] * angle;
    float roll = axis[2] * angle;
    float yaw = axis[0] * angle;
    
    // calculate pwms for rotates
    float angleScale = Constants.PWM_SCALE * -4 / (2 * (float)Math.PI);

    final float mixPitchRoll = 0.1f;
    final float mixYawRoll = 0.4f;
    final float mixThrottleUp = -0.5f;
    
    pwm[0] = roll * angleScale;
    pwm[1] = (pitch + mixPitchRoll * Math.abs(roll)) * angleScale;
    pwm[2] = (throttle * 2 * (1 + mixThrottleUp * up[2])) - 1f;
    pwm[3] = (yaw + mixYawRoll * roll) * angleScale;
    pwm[4] = aux;
    
//    if (Math.random() < 0.01) {
//      AbcdActivity.appendStatus("throttle " + throttle);
//    }
    
    // trim pwms
    for (int index = 0; index < pwm.length; ++index) {
      pwm[index] = trim(pwm[index], trim[index]);
    }
  }
  
//  public void mix(float from, float to, float mix) {
//    float mixed = (from * (1 + to * mix)) + (from * to * mix); 
//  }
  
  public static float trim(float pwm, int trimIndex) {
    float trimValue = (trimIndex / (0.5f * UiEngine.THROTTLE_MAX)) - 1;
    return trim(pwm, trimValue);
  }
  public static float trim(float pwm, float trim) {
    float trimmedPwm = (1 - Math.abs(trim)) * pwm + trim;
    return trimmedPwm;
  }
  public void zero() {
    zeroed = false;
  }
  
  private void doZero(float[] accelNorm) {
    if (!zeroed) {
      zeroX = accelNorm.clone();

      float dZeroDotY = Vec.computeDotProduct(zeroX, deviceY);
      final float[] deviceHorizontal;
      if (Math.abs(dZeroDotY) < Math.pow(2, -0.5)) {
        deviceHorizontal = deviceY;
      } else {
        deviceHorizontal = deviceX;
      }
      zeroZ = Vec.normalize(Vec.computeCrossProduct(zeroX, deviceHorizontal));
      zeroY = Vec.normalize(Vec.computeCrossProduct(zeroX, zeroZ));

      zeroed = true;
    }
  }
  
  private static void map(float[] vec, float[] zeroX, float[] zeroY, float[] zeroZ, float[] map) {
    map[0] = (float) Vec.computeDotProduct(vec, zeroX);
    map[1] = (float) Vec.computeDotProduct(vec, zeroY);
    map[2] = (float) Vec.computeDotProduct(vec, zeroZ);
  }
}
