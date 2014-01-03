package com.thomastechnics.abcd;


public class SensorDataListener extends DataListener {
    private long lastTime;
    private final static float[] deviceX = { 1, 0, 0 };
    private final static float[] deviceY = { 0, 1, 0 };
    private float[] zeroX;
    private float[] zeroY;
    private float[] zeroZ;
    boolean zeroed;
    private final DataEngine dataEngine;
    private final String id;
    
    public SensorDataListener(DataEngine dataEngine, String id) {
      this.dataEngine = dataEngine;
      this.id = id;
    }

    @Override
    public void onDataChange(String id, ByteData data) {
      final long time = System.currentTimeMillis(); 
      if (time - lastTime > Constants.SENSOR_REPEAT) {
        this.lastTime = time;
//        if (isModel) 
        {
          // calculate sensor in flat ufl space
          // calculate rotates from ufl sensor to goal
          // calculate pwms for rotates
          // set pwms
        }
//          if (isControl) 
        {
          // calculate sensor in flat ufl space
          // set ufl sensor as goal
        }
      }
    }
    
    public void zero() {
      zeroed = false;
    }
    
    private void updateModel(ByteData data) {
      // if (SensorManager.getRotationMatrix(rotation, inclination, accel, mag))
      // {
      // SensorManager.getOrientation(rotation, orientation);
      // float ig = (float)(1 / SensorManager.GRAVITY_EARTH);
      // float im = (float)(1 / Math.PI);
      // }
      
      final float[] accel = new float[3];
      ByteUtil.getFloats(accel, data.bytes);
      
      final float[] accelNorm;
      if (Vec.length(accel) > 0) {
        accelNorm = Vec.normalize(accel.clone());
      } else {
        accelNorm = new float[] { 0, 0, 0 };
      }

      if (!zeroed) {
        zeroX = accelNorm.clone();
        zeroed = true;

//        float dZeroDotX = Vec.computeDotProduct(zeroX, deviceX);
        float dZeroDotY = Vec.computeDotProduct(zeroX, deviceY);
//        if (Math.abs(dZeroDotY) < Math.abs(dZeroDotX)) {
        if (Math.abs(dZeroDotY) < Math.pow(2, -0.5)) {
          zeroZ = Vec.computeCrossProduct(zeroX, deviceY);
        } else {
          zeroZ = Vec.computeCrossProduct(zeroX, deviceX);
        }
        Vec.normalize(zeroZ);

        zeroY = Vec.normalize(Vec.computeCrossProduct(zeroX, zeroZ));
      }

      float dXd = (float) Vec.computeDotProduct(accelNorm, zeroX);
      float dYd = (float) Vec.computeDotProduct(accelNorm, zeroY);
      float dZd = (float) Vec.computeDotProduct(accelNorm, zeroZ);

      float[] zeroAccel = { dXd, dYd, dZd };

//      dataEngine.update(DataId.SENSOR, new SensorUpdate(zeroAccel, timestamp, 1));
    }
  }