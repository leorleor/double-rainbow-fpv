package com.thomastechnics.abcd;

import java.util.Arrays;

public class ActionEngine {
  public enum Actions {
    ACTION_NONE, 
    ACTION_KILL, 
    ACTION_LOG, 
    ACTION_CAMERA, 
    ACTION_PREVIEW, 
    ACTION_HUD, 
    ACTION_DISPLAY, 
    ACTION_DISPLAY_OFF, 
    ACTION_DISPLAY_ON, 
    ACTION_TEST, 
    ACTION_HOLD, 
    ACTION_CONNECT, 
    ACTION_FLAT, 
    ACTION_THROTTLE, 
    ACTION_SPIN, 
    ACTION_LIFTS, 
    ACTION_LIFTOFF, 
    ACTION_SIM, 
    ACTION_SUP, 
    ACTION_MIN, 
    ACTION_DOWN, 
    ACTION_MID, 
    ACTION_UP, 
    ACTION_MAX, 
    ACTION_MAN,
    ACTION_ALIGN,
    ACTION_HELP,
    ACTION_ZERO, 
    ACTION_SETUP, 
    ACTION_STOP,
    ACTION_GLIDE,
    ACTION_GLIDE_ON,
    ACTION_GLIDE_OFF,
    ACTION_STEER,
    ACTION_STEER_ON,
    ACTION_STEER_OFF,
    ACTION_RECORD,
    ACTION_RECORD_ON,
    ACTION_RECORD_OFF,
    ;

    public final String text;

    private Actions() {
      this(null);
    }

    private Actions(String text) {
      this.text = text;
    }
  }
  
  private final AbcdActivity activity;
  public ActionEngine(AbcdActivity activity) {
    this.activity = activity;
  }
  
  
  void actHold() {
    if (activity.transferListener != null) {
      final byte[] data = { 
          '.',  
          ' ', ' ', ' ',
          ' ', ' ', ' ',
          '.',  
//          'S', '2', 'a',
//          'S', '3', 'z',
//          '?',
      };
      activity.transferListener.write(data);
    }
  }
  
  
  public void actPreviewSize(int sizeIndex) {
    activity.updateCameraPixels(sizeIndex);
  }
  
  // Action handling methods. 

  public void act(Actions action) {
    activity.appendStatus("act(" + action + ")");

    try {
      if (action == Actions.ACTION_NONE) {
        // yawns
      } else if (action == Actions.ACTION_KILL) {
        activity.restartUsb();
      } else if (action == Actions.ACTION_CONNECT) {
//        actConnect();
      } else if (action == Actions.ACTION_HOLD) {
        actHold();
      } else if (action == Actions.ACTION_TEST) {
        actTest();
      } else if (action == Actions.ACTION_ZERO) {
        activity.zeroControl();
      } else if (action == Actions.ACTION_LOG) {
        actLand();
      } else if (
          (action == Actions.ACTION_DISPLAY) ||
          (action == Actions.ACTION_THROTTLE) ||
          (action == Actions.ACTION_SETUP) ||
          (action == Actions.ACTION_CAMERA) ||
          (action == Actions.ACTION_PREVIEW) ||
          (action == Actions.ACTION_HUD) ||
          (action == Actions.ACTION_HELP) 
          ) {
        activity.getUi().toggleVisible(action);
      } else if (action == Actions.ACTION_FLAT) {
        actFlat();
      } else if (action == Actions.ACTION_LIFTS) {
      } else if (action == Actions.ACTION_LIFTOFF) {
      } else if (action == Actions.ACTION_SIM) {
        actSim();
      } else if (action == Actions.ACTION_SUP) {
      } else if (action == Actions.ACTION_MIN) {
        activity.setThrottle(0);
      } else if (action == Actions.ACTION_DOWN) {
//        activity.incThrottle(-0.05f);
      } else if (action == Actions.ACTION_MID) {
        activity.setThrottle(0.5f);
      } else if (action == Actions.ACTION_UP) {
//        activity.incThrottle(0.05f);
      } else if (action == Actions.ACTION_MAX) {
        activity.setThrottle(1);
      } else if (action == Actions.ACTION_MAN) {
      } else if (action == Actions.ACTION_ALIGN) {
        activity.align();
      }
    } catch (Exception e) {
      activity.appendStatus(e.toString());
      activity.appendStatus(Arrays.deepToString(e.getStackTrace()));
    }
  }

  private void actFlat() {
    // tare orientation and gravity
    activity.zeroModel();
  }

  public void actThrottle(float throttle) {
    activity.setThrottle(throttle);
  }

  private void actTest() {
    activity.onUsb();
    activity.loopNetwork(false);
  }

  private void actLand() {
    activity.toggleFreezeAppend();
  }

  private void actSim() {
    activity.onUsb();
  }

  private void actHome() {
  }
}
