package com.thomastechnics.abcd;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VerticalSeekBar;

import com.thomastechnics.abcd.ActionEngine.Actions;
import com.thomastechnics.abcd.DataEngine.DataUpdate;

public class UiEngine {
  private static final int CAMERA_WEIGHT = 10;
  public static final int ORANGE = Color.rgb(255, 127, 0);
//  public static final int ORANGE = Color.rgb(240, 96, 10);
  public static final int VIOLET = Color.HSVToColor(new float[] {280, 0.7f, 0.7f});
  private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
  private static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
  private static final int THROTTLE_OFFSET = 3 * ByteUtil.INT_SIZE;

  private static class CommandBarListener implements SeekBar.OnSeekBarChangeListener {
    private final DataEngine dataEngine;
    private final TextView text;
    private final SetupSpec spec;

    private CommandBarListener(DataEngine dataEngine, TextView text, SetupSpec spec) {
      this.dataEngine = dataEngine;
      this.text = text;
      this.spec = spec;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (fromUser) {
        if (spec.command >= 0) {
          int[] values = {
              spec.command,
              progress,
          };
          ArrayDataUpdate update = new ArrayDataUpdate(DataId.CONTROL, DataId.COMMAND, values, true, DataId.TIME);
          update.doUpdate(dataEngine, System.currentTimeMillis());
        }

        if (text != null) {
          float value = spec.min + (((spec.max - spec.min) * progress) / (spec.steps - 1));
          String labelText = formatSetupLabel(spec, value);
          if (!labelText.equals(text.getText())) {
            text.setText(labelText);
          }
        }
      }
    }
  }

  private void setThrottle(int throttle) {
    sensorThrottle.setProgress(throttle);
  }
  private int getThrottle() {
    return sensorThrottle.getProgress();
  }
  
  private class ThrottleClickListener implements OnClickListener {
    private final DataEngine dataEngine;
    private float value;
    private boolean relative;
    private int command;

    public ThrottleClickListener(DataEngine dataEngine, float value, boolean relative) {
      this.dataEngine = dataEngine;
      this.value = value;
      this.command = DataId.COMMAND_THROTTLE;
      this.relative = relative;
    }

    @Override
    public void onClick(View v) {
      if (command >= 0) {
        float commandValue = value;
        int intValue = (int)(commandValue * THROTTLE_MAX);
        if (relative) {
          intValue += getThrottle();
        }
        int[] values = {
            command,
            intValue,
        };
        ArrayDataUpdate update = new ArrayDataUpdate(DataId.CONTROL, DataId.COMMAND, values, true, DataId.TIME);
        update.doUpdate(dataEngine, System.currentTimeMillis());
        setThrottle(intValue);
      }
    }
  }

  private static String formatSetupLabel(SetupSpec spec, Float value) {
    String label = String.format(Locale.US, 
        "%s (%.1f to %.1f): %.3f", 
        spec.name, spec.min, spec.max, value);
    return label;
  }

  private final class DataClickListener implements OnClickListener {
    private final float value;
    private final String id;
    private final int offset;
    private final boolean absolute;

    private DataClickListener(String id, int offset, float value, boolean absolute) {
      this.value = value;
      this.id = id;
      this.offset = offset;
      this.absolute = absolute;
    }

    @Override
    public void onClick(View v) {
      dataEngine.update(id, new FloatDataUpdate(offset, value, absolute));
    }
  }

  private final class ThrottleListener extends DataListener {
    private ProgressBar slider;
    private ThrottleListener(ProgressBar slider) {
      this.slider = slider;
    }

    @Override
    public void onDataChange(String id, ByteData data) {
      final int command = ByteUtil.getInt(DataId.COMMAND, data.bytes);
      if (command == DataId.COMMAND_THROTTLE) {
        final int progress = ByteUtil.getInt(DataId.DATA, data.bytes);
        if (slider.getProgress() != progress) {
          slider.post(new Runnable() {
            @Override
            public void run() {
              slider.setProgress(progress);
            }
          });
        }
      }
    }
  }

  private final class ServoListener extends DataListener {
    private final int max;
    private ProgressBar slider;
    private final int offset;
    private ServoListener(int max, ProgressBar slider, int offset) {
      this.max = max;
      this.slider = slider;
      this.offset = offset;
    }
    private ServoListener(int max, ProgressBar slider) {
      this.max = max;
      this.slider = slider;
      this.offset = THROTTLE_OFFSET;
    }

    @Override
    public void onDataChange(String id, ByteData data) {
      float throttle = ByteUtil.getFloat(offset, data.bytes);
      final int progress = convertToProgress(throttle, max);
      if (slider.getProgress() != progress) {
        slider.post(new Runnable() {
          @Override
          public void run() {
            slider.setProgress(progress);
          }
        });
      }
    }
  }

  public static int convertToProgress(float value, int max) {
    int progress;
    if (max > 0) {
      progress = (int)(0.5f * max * (value + 1));
    } else {
      progress = (-max) - (int)(0.5f * (-max) * (value + 1));
    }
    return progress;
  }

  public static float convertFromProgress(int progress, int max) {
    float value = ((progress * 2f) / max) - 1;
    return value;
  }

  private static class ButtonSpec {
    final String text;
    final int color;
    final Actions action;
    final Actions actionOff;
    final boolean isToggle;
    final String onText;
    final int type;
    final int value;

    private ButtonSpec(String text, int id, int color, Actions action, boolean isToggle, String onText) {
      //      this(text, id, color, action, isToggle, onText, null, 0, 0);
      this(text, id, color, action, isToggle, onText, TYPE_NONE, 0);
    }
    private ButtonSpec(String text, int id, int color, Actions action, boolean isToggle, String onText, int type, int value) {
      this(text, id, color, action, isToggle, onText, type, value, Actions.ACTION_NONE);
    }
    private ButtonSpec(String text, int id, int color, Actions action, boolean isToggle, String onText, int type, int value, Actions actionOff) {
      this.text = text;
      this.color = color;
      this.action = action;
      this.actionOff = actionOff;
      this.onText = onText;
      this.isToggle = isToggle;
      this.type = type;
      this.value = value;
    }
    private ButtonSpec(String text, int id, int color, Actions action, boolean isToggle) {
      this(text, id, color, action, isToggle, text);
    }
    private ButtonSpec(String text, int id, int color, Actions action) {
      this(text, id, color, action, false, null);
    }

    public String name() {
      return text;
    }
  }

  private class SpecClickListener implements OnClickListener {
    private final ButtonSpec spec;

    private SpecClickListener(ButtonSpec spec) {
      this.spec = spec;
    }

    @Override
    public void onClick(View view) {
      if (spec.type == TYPE_MODE) {
        setMode(spec.value);
      } else if (spec.type == TYPE_VIEW) {
        ToggleButton toggle = (ToggleButton) view;
        setViewVisible(spec.action, toggle.isChecked());
      } else {
        ToggleButton toggle = (ToggleButton) view;
        if (toggle.isChecked()) {
          doAction(spec.action);
        } else {
          doAction(spec.actionOff);
        }
      }
    }

  }
  private class SpecTouchListener implements OnTouchListener {
    private final ButtonSpec spec;

    private SpecTouchListener(ButtonSpec spec) {
      this.spec = spec;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        doAction(spec.action);
      } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
//        doAction(spec.actionOff);
      } else if (event.getAction() == MotionEvent.ACTION_UP) {
        doAction(spec.actionOff);
      }

      return false;
    }
  }

  float glideOffThrottle = 0f;
  public void doAction(Actions action) {
    AbcdActivity.appendStatus("doAction(" + action.name() + ")");
    if (action == Actions.ACTION_NONE) {

    } else if (action == Actions.ACTION_GLIDE_ON) {
      updateCommand(DataId.COMMAND_THROTTLE, 0f);
    } else if (action == Actions.ACTION_GLIDE_OFF) {
      updateCommand(DataId.COMMAND_THROTTLE, glideOffThrottle);
      
    } else if (action == Actions.ACTION_STEER_ON) {
      activity.setSteer(true);
    } else if (action == Actions.ACTION_STEER_OFF) {
      activity.setSteer(false);

    } else if (action == Actions.ACTION_RECORD_ON) {
      updateCommand(DataId.COMMAND_REC, 1);
    } else if (action == Actions.ACTION_RECORD_OFF) {
      updateCommand(DataId.COMMAND_REC, 0);

    } else if (action == Actions.ACTION_SETUP) {
      setViewVisible(Actions.ACTION_SETUP, false);
    } else if (action == Actions.ACTION_ZERO) {
      activity.zeroControl();
    } else if (action == Actions.ACTION_LOG) {
      activity.toggleFreezeAppend();
    } else if (action == Actions.ACTION_SIM) {
      activity.onUsb();
    } else if (action == Actions.ACTION_TEST) {
      activity.onUsb();
      activity.loopNetwork(false);
    }    
  }

  private Map<Integer, ToggleButton> modeButtonMap = new HashMap<Integer, ToggleButton>();
  private void setMode(final int mode) {
    dataEngine.update(DataId.CONTROL, new DataUpdate() {
      @Override
      public void update(String id, ByteData data) {
        ByteUtil.putInt(DataId.COMMAND_MODE, DataId.COMMAND, data.bytes);
        ByteUtil.putInt(mode, DataId.DATA, data.bytes);
      }
    });

    for (Map.Entry<Integer, ToggleButton> entry : modeButtonMap.entrySet()) {
      ToggleButton button = entry.getValue();
      boolean checked = (entry.getKey() == mode);
      if (checked != button.isChecked()) {
        button.setChecked(checked);
      }
      if (entry.getKey() == DataId.MODE_ON) {
        if (checked) {
          setButtonColor(button, Color.GREEN);
        } else {
          setButtonColor(button, ORANGE);
        }
      }
    }
  }

  Map<Integer, ToggleButton> viewButtonMap = new HashMap<Integer, ToggleButton>();
  Map<Integer, View> viewMap = new HashMap<Integer, View>();
  public void setViewVisible(Actions action, boolean visible) {
    int id = action.ordinal();
    AbcdActivity.appendStatus("setViewVisible(" + id + ", " + visible + ")");
    View view = viewMap.get(id);
    AbcdActivity.appendStatus("setViewVisible(" + view + ", " + visible + ")");
    if (view != null) {
      final int visibility;
      if (visible) {
        visibility = View.VISIBLE;
      } else {
        visibility = View.GONE;
      }
      if (view.getVisibility() != visibility) {
        view.setVisibility(visibility);
        updateViewButton(id, visible);
      }
    }
  }
  
  private void updateViewButton(int id, 
      boolean visible
      ) {
    ToggleButton button = viewButtonMap.get(id);
    if (button != null) {
      if (button.isChecked() != visible) {
        button.setChecked(visible);
      }
    }
  }

  private void updateCommand(int command, float value) {
    updateCommand(command, Float.floatToRawIntBits(value));
  }
  private void updateCommand(final int command, final int data) {
    if (command >= 0) {
      int[] values = {
          command,
          data,
      };
      ArrayDataUpdate update = new ArrayDataUpdate(DataId.CONTROL, DataId.COMMAND, values, true, DataId.TIME);
      update.doUpdate(dataEngine, System.currentTimeMillis());
    }
  }

  private final ButtonSpec[] actionSpecs = new ButtonSpec[] {
      new ButtonSpec("HOME", 4, Color.MAGENTA, Actions.ACTION_NONE, true, "Homing", TYPE_MODE, DataId.MODE_HOME),
      new ButtonSpec("K!LL", 4, Color.RED, Actions.ACTION_NONE, true, "Killed", TYPE_MODE, DataId.MODE_KILL),
      new ButtonSpec("ON!", 4, ORANGE, Actions.ACTION_NONE, true, "ON.", TYPE_MODE, DataId.MODE_ON),
      new ButtonSpec("GLIDE", 4, Color.YELLOW, Actions.ACTION_NONE, true, "Gliding"),
      new ButtonSpec("STEER", 3, Color.YELLOW, Actions.ACTION_STEER_ON, true, null, TYPE_ACTION, 0, Actions.ACTION_STEER_OFF),
      new ButtonSpec("ZER0", 4, Color.YELLOW, Actions.ACTION_ZERO, false),
      new ButtonSpec("RECORD", 4, Color.GREEN, Actions.ACTION_RECORD_ON, true, null, TYPE_ACTION, 0, Actions.ACTION_RECORD_OFF),
      new ButtonSpec("DISPLAY", 4, Color.CYAN, Actions.ACTION_DISPLAY, true, null, TYPE_VIEW, 1),
  };

  private final ButtonSpec[] displaySpecs = new ButtonSpec[] {
      //      new ButtonSpec("HELP", 4, Color.CYAN, Actions.ACTION_NONE, true),
      new ButtonSpec("CAMERA", 4, Color.RED, Actions.ACTION_PREVIEW, true, null, TYPE_VIEW, 1),
      new ButtonSpec("FPV", 3, Color.RED, Actions.ACTION_CAMERA, true, null, TYPE_VIEW, 1),
      new ButtonSpec("HUD", 4, ORANGE, Actions.ACTION_HUD, true, null, TYPE_VIEW, 1),
      //      new ButtonSpec("DASH", 3, ORANGE, Actions.ACTION_NONE, true),
      new ButtonSpec("THROTTLE", 4, Color.YELLOW, Actions.ACTION_THROTTLE, true, null, TYPE_VIEW, 1),
      new ButtonSpec("SETUP", 4, Color.YELLOW, Actions.ACTION_SETUP, true, null, TYPE_VIEW, 1),
      //      new ButtonSpec("NETWORK", 4, Color.YELLOW, Actions.ACTION_NONE, true),
      new ButtonSpec("LOG", 3, Color.GREEN, Actions.ACTION_LOG),
//      new ButtonSpec("RECORD", 4, Color.GREEN, Actions.ACTION_NONE, true, null, TYPE_ACTION, 0, Actions.ACTION_RECORD_OFF),
      new ButtonSpec("SIM USB", 1, Color.CYAN, Actions.ACTION_NONE, false, null, TYPE_ACTION, 0, Actions.ACTION_SIM),
      new ButtonSpec("SIM ALL", 1, Color.CYAN, Actions.ACTION_NONE, false, null, TYPE_ACTION, 0, Actions.ACTION_TEST),
//      new ButtonSpec("SIM ALL", 2, Color.CYAN, Actions.ACTION_TEST),
  };



  //  private RelativeLayout.LayoutParams relParams;
  private LinearLayout.LayoutParams colParams;
  private final AbcdActivity activity;
  private final ActionEngine actionEngine;
  private final LinearLayout contentRow;
  private View actionView;
  private View displayView;
  private View throttleView;
  private View setupView;
  private VerticalSeekBar modelThrottle;
  private VerticalSeekBar controlThrottle;
  private DataEngine dataEngine;
  private VerticalSeekBar sensorThrottle;

  public UiEngine(AbcdActivity activity, LinearLayout contentRow, ActionEngine actionEngine, DataEngine dataEngine) {
    this.contentRow = contentRow;
    //    this.activity = activity;
    this.activity = activity;
    this.actionEngine = actionEngine;
    this.dataEngine = dataEngine;
  }

  public View createActions() {
    return createButtons(activity, actionEngine, actionSpecs);
  }
  public View createDisplays() {
    return createButtons(activity, actionEngine, displaySpecs);
  }
  private View createButtons(Context context, ActionEngine actionEngine, ButtonSpec[] specs) {
    LinearLayout buttonColumn = new LinearLayout(context);
    buttonColumn.setOrientation(LinearLayout.VERTICAL);

    addButtons(specs, buttonColumn);

    return buttonColumn;
  }

  private void addButtons(ButtonSpec[] specs, LinearLayout buttonColumn) {
    for (final ButtonSpec spec : specs) {
      final Button button = createButton(spec);
      
      // adds to any appropriate index
      if (spec.type == TYPE_MODE) {
        modeButtonMap.put(spec.value, (ToggleButton)button);
      } else if (spec.type == TYPE_VIEW) {
        viewButtonMap.put(spec.action.ordinal(), (ToggleButton)button);
        if (spec.value > 0) {
          ((ToggleButton)button).setChecked(true);
        }
      }

      // adds a listener
      if (spec.isToggle) {
        button.setOnClickListener(new SpecClickListener(spec));
      } else {
        button.setOnTouchListener(new SpecTouchListener(spec));
      }

      buttonColumn.addView(button);
    }
  }

  private Button createButton(ButtonSpec spec) {
    final Button button;
    if (spec.isToggle) {
      ToggleButton toggle = new ToggleButton(activity);
      toggle.setTextOff(spec.name());
      toggle.setTextOn(spec.onText);
      button = toggle;
    } else {
      button = new Button(activity);
    }

    button.setText(spec.name());
    button.setTextSize(12);
    int color = spec.color;
    setButtonColor(button, color);

    colParams = new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1);
    button.setLayoutParams(colParams);

    return button;
  }

  public static void setButtonColor(View view, int color) {
    Drawable toFilter = view.getBackground();
    if (toFilter != null) {
      int filterColor = Color.argb(111, Color.red(color), Color.green(color), Color.blue(color));
      toFilter.setColorFilter(filterColor, PorterDuff.Mode.DST_OVER);
      // toFilter.setColorFilter(filterColor, PorterDuff.Mode.LIGHTEN);
    } else {
      int filterColor = Color.argb(31, Color.red(color), Color.green(color), Color.blue(color));
      view.setBackgroundColor(filterColor);
    }
  }

  public void createUi() {
    actionView = addUi(createActions(), 0, 0);
    displayView = addUi(createDisplays(), 0, 0);
    throttleView = addUi(createThrottle(), 0, 0);
    setupView = addUi(createSetup(), 0, 10);
    createCameraFrame(false);
    createCameraFrame(true);
    
    viewMap.put(Actions.ACTION_DISPLAY.ordinal(), displayView);
    viewMap.put(Actions.ACTION_THROTTLE.ordinal(), throttleView);
    viewMap.put(Actions.ACTION_SETUP.ordinal(), setupView);
    viewMap.put(Actions.ACTION_CAMERA.ordinal(), cameraStack);
    viewMap.put(Actions.ACTION_PREVIEW.ordinal(), previewStack);
    viewMap.put(Actions.ACTION_HUD.ordinal(), cameraHud);
    
    setViewVisible(Actions.ACTION_DISPLAY, false);
    setViewVisible(Actions.ACTION_THROTTLE, false);
    setViewVisible(Actions.ACTION_CAMERA, false);
    setViewVisible(Actions.ACTION_PREVIEW, false);
  }

  public View addUi(View view, int index, int weight) {
    final int width;
    if (weight == 0) {
      width = WRAP_CONTENT;
    } else {
      width = 0;
    }
    addChild(contentRow, view, width, MATCH_PARENT, weight, index);
    return view;
  }

  public View createTrim() {
    LinearLayout column = new LinearLayout(activity);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

    //    RelativeLayout row = createServoSliders();
    //    addChild(column, row, WRAP_CONTENT, 0, 5, ADD_END);

    return column;
  }

  public static final int TYPE_NONE = 0;
  public static final int TYPE_LABEL = 1;
  public static final int TYPE_INT = 2;
  public static final int TYPE_STRING = 3;
  public static final int TYPE_MODE = 4;
  public static final int TYPE_VIEW = 5;
  public static final int TYPE_ACTION = 6;

  private static class SetupSpec {
    public final String name;
    //    public final String id;
    public final int command;
    public final int type;
    public final float min;
    public final float max;
    public final int steps;
    public final float init;
    public SetupSpec(String name, int type, int offset, float min, float max, int steps) {
      this(name, type, offset, min, max, steps, (min + max) / 2);
    }
    public SetupSpec(String name, int type, int offset, float min, float max, int steps, float init) {
      super();
      this.name = name;
      //      this.id = id;
      this.command = offset;
      this.type = type;
      this.min = min;
      this.max = max;
      this.steps = steps;
      this.init = init;
    }
  }

  private static final int MAX_STEPS = 256 * 1024;
  private static final int PORT_COUNT = 64 * 1024;
  public final SetupSpec[] setupSpecs = {
      new SetupSpec("Control", TYPE_LABEL, -1, 0, 0, 0),
      new SetupSpec("Server", TYPE_STRING, -1, 0, 0, 0),
      new SetupSpec("Port", TYPE_INT, -1, 0, PORT_COUNT - 1, PORT_COUNT, 6969),

      new SetupSpec("Camera", TYPE_LABEL, -1, 0, 0, 0),
      //    new SetupSpec("Pixels", TYPE_FLOAT, "CAM_PIXELS", 0, 1080, 1081),
      new SetupSpec("Pixels", TYPE_INT, DataId.SETUP_CAM_PIXELS, 0, 15, 16, 2),
      new SetupSpec("Quality", TYPE_INT, DataId.SETUP_CAM_QUALITY, 0, 100, 101, Constants.PREVIEW_JPEG_QUALITY),
      new SetupSpec("Zoom", TYPE_INT, DataId.SETUP_CAM_ZOOM, 0, 60, 61, 0),
      new SetupSpec("Rec!", TYPE_INT, DataId.COMMAND_REC, 0, 1, 2, 0),

      new SetupSpec("Homing", TYPE_LABEL, -1, 0, 0, 0),
      //      new SetupSpec("Lat", TYPE_INT, DataId.SETUP_HOME_LAT, -90, 90, 1 + (180 * 100)),
      //      new SetupSpec("Lon", TYPE_INT, DataId.SETUP_HOME_LON, -180, 180, 1 + (360 * 100)),
      //      new SetupSpec("Alt", TYPE_INT, DataId.SETUP_HOME_ALT, 0, 1023, 1024),
      new SetupSpec("Home?", TYPE_INT, DataId.COMMAND_HOME, 0, 1, 2, 0),

      //      new SetupSpec("Flat", TYPE_LABEL, -1, 0, 0, 0),
      //      new SetupSpec("Up X", TYPE_INT, DataId.SETUP_FLAT_UP_X, -1, 1, 1024),
      //      new SetupSpec("Up Y", TYPE_INT, DataId.SETUP_FLAT_UP_Y, -1, 1, 1024),
      //      new SetupSpec("Up Z", TYPE_INT, DataId.SETUP_FLAT_UP_Z, -1, 1, 1024),
      new SetupSpec("Attitude", TYPE_LABEL, -1, 0, 0, 0),
      new SetupSpec("Flat?", TYPE_INT, DataId.COMMAND_FLAT, 0, 1, 2, 0),

      new SetupSpec("Trim", TYPE_LABEL, -1, 0, 0, 0),
      new SetupSpec("Aileron", TYPE_INT, DataId.SETUP_TRIM_AILERON, -1, 1, 1024),
      new SetupSpec("Elevator", TYPE_INT, DataId.SETUP_TRIM_ELEVATOR, -1, 1, 1024),
      new SetupSpec("Throttle", TYPE_INT, DataId.SETUP_TRIM_THROTTLE, -1, 1, 1024),
      new SetupSpec("Rudder", TYPE_INT, DataId.SETUP_TRIM_RUDDER, -1, 1, 1024),
      new SetupSpec("Aux", TYPE_INT, DataId.SETUP_TRIM_AUX, -1, 1, 1024),
  };

  public View createSetup() {
    LinearLayout column = new LinearLayout(activity);
    column.setOrientation(LinearLayout.VERTICAL);
    //    column.setHorizontalGravity(Gravity.FILL_HORIZONTAL);

    for (final SetupSpec spec : setupSpecs) {
      LinearLayout.LayoutParams rel;

      final TextView label;
      {
        TextView text = new TextView(activity);
        text.setInputType(InputType.TYPE_NULL);
        rel = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);

        if (spec.type == TYPE_INT) {
          text.setText(formatSetupLabel(spec, spec.init));
        } else {
          text.setText(spec.name);
        }

        if (spec.type == TYPE_LABEL) {
          rel.setMargins(5, 5, 0, 0);
          text.setTextSize(14);
        } else {
          rel.setMargins(20, 0, 0, 0);
          text.setTextSize(12);
        }

        column.addView(text, rel);
        label = text;
      } 

      SeekBar bar = null;
      if (spec.type == TYPE_INT) {
        int max = spec.steps - 1;
        int progress = (int)(max * (spec.init - spec.min) / (spec.max - spec.min));
        bar = createSlider(max, 0, 0, progress, true, false);
        //        dataEngine.listen(DataId.SENSOR, new ThrottleDataListener(max, bar, 4));

        //        LinearLayout row = new LinearLayout(context);
        //        row.setOrientation(LinearLayout.HORIZONTAL);
        //        rel = new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        //        row.addView(bar, rel);

        rel = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        rel.setMargins(20, -10, 0, -0);
        //         rel.addRule(RelativeLayout.CENTER_HORIZONTAL);
        column.addView(bar, rel);
        //         column.addView(row, rel);

        bar.setOnSeekBarChangeListener(new CommandBarListener(dataEngine, label, spec));
        
        
        final SeekBar modelBar = createSlider(max, 0, 0, progress, true, false);
        dataEngine.listen(DataId.MODEL, new DataListener() {
          @Override
          public void onDataChange(String id, ByteData data) {
            final int command = ByteUtil.getInt(DataId.COMMAND, data.bytes);
            if (command == spec.command) {
              final int value = ByteUtil.getInt(DataId.DATA, data.bytes);
              activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  modelBar.setProgress(value);
                }
              });
            }
          }
        });

        //        LinearLayout row = new LinearLayout(context);
        //        row.setOrientation(LinearLayout.HORIZONTAL);
        //        rel = new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        //        row.addView(bar, rel);
        modelBar.setEnabled(false);
        rel = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        rel.setMargins(20, -40, 0, -0);
        //         rel.addRule(RelativeLayout.CENTER_HORIZONTAL);
        column.addView(modelBar, rel);
        

      }

      if (spec.type == TYPE_STRING) {
        //|| spec.type == TYPE_INT) {
        final View textView;
//        if (spec.type == TYPE_INT) {
          //          final TextView text = new TextView(context);
          //          final EditText text = new EditText(context);
          //          final int inputType;
          //          text.setHint(spec.name);
          //          inputType = 
          //              InputType.TYPE_CLASS_NUMBER | 
          //              InputType.TYPE_NUMBER_FLAG_DECIMAL | 
          //              InputType.TYPE_NUMBER_FLAG_SIGNED;
          //          text.setInputType(inputType);
          //          textView = text;
          // bar.setOnSeekBarChangeListener(new CommandBarListener(dataEngine, text, spec));

//          textView = null;

//        } else {
          final EditText text = new EditText(activity);
          final int inputType;
          final String hint;
          if (spec.name.equals("Server")) {
            hint = "Ignored.  These r hardcoded for now.";
          } else {
            hint = spec.name;
          }
          text.setHint(hint);
          inputType = 
              InputType.TYPE_CLASS_TEXT | 
              InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
          text.setInputType(inputType);
          textView = text;
//        }

//        if (textView != null) {
          rel = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
          rel.setMargins(20, -10, 0, 0);
          //        rel.addRule(RelativeLayout.CENTER_HORIZONTAL);
          column.addView(textView, rel);
//        }
      } 

    }

    ButtonSpec spec = new ButtonSpec("Setup Complete", 4, Color.YELLOW, Actions.ACTION_SETUP, false, null, TYPE_ACTION, 0);
//    Button button = createButton(spec);
    addButtons(new ButtonSpec[] {spec}, column);
//    button.setOnClickListener(new OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        setViewVisible(action, visible)
//      }
//    });
//    column.addView(button);
    //    RelativeLayout row = createServoSliders();
    //    addChild(column, row, WRAP_CONTENT, 0, 5, ADD_END);

    //    return column;
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(column);
    return scroll;
  }

  public void createHud() {

  }

  private FrameLayout cameraFrame;
  private RelativeLayout createCameraView(View cameraView, LinearLayout hud) {
    RelativeLayout view = new RelativeLayout(activity);

    RelativeLayout.LayoutParams rel;
    rel = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);

    //    View cameraView = new FrameLayout(context);
    view.addView(cameraView, rel);

    hud.setOrientation(LinearLayout.VERTICAL);
    //    hud.setAlpha(0.9f);
    //    rebuildHud(hud);
    view.addView(hud, rel);

    return view;
  }

  private static class HudSpec {
    //    public String id;
    public float value;
    public long time;

    public HudSpec(float value, long time) {
      this.value = value;
      this.time = time;
    }
  }

  private static final HudSpec EMPTY_HUD_SPEC = new HudSpec(0, 0);

  //  private Map<String, HudSpec> hudMap = new TreeMap<String, HudSpec>();
  private Map<String, HudSpec> hudMap = new LinkedHashMap<String, HudSpec>();
  public void setHudValue(String id, float value, long time) {
    boolean contains = hudMap.containsKey(id);

    HudSpec spec;
    if (contains) {
      spec = hudMap.get(id);
      spec.time = time;
      spec.value = value;
    } else {
      spec = new HudSpec(value, time);
      hudMap.put(id, new HudSpec(value, time));
      //      rebuildHud(cameraHud);
      //      rebuildHud(previewHud);
    }

  }

  private RebuildHudRunnable cameraHudRunnable;
  private RebuildHudRunnable previewHudRunnable;
  private void updateHud() {
    if (cameraHudRunnable == null) {
      if (cameraStack != null) {
        if (cameraStack.getVisibility() == View.VISIBLE) {
          cameraHudRunnable = new RebuildHudRunnable(cameraHud);
          activity.runOnUiThread(cameraHudRunnable);
        }
      }
    }
    if (previewHudRunnable == null) {
      if (previewStack != null) {
        if (previewStack.getVisibility() == View.VISIBLE) {
          previewHudRunnable = new RebuildHudRunnable(previewHud);
          activity.runOnUiThread(previewHudRunnable);
        }
      }
    }
  }

  private class RebuildHudRunnable implements Runnable {
    private final LinearLayout hud;

    public RebuildHudRunnable(LinearLayout hud) {
      this.hud = hud;
    }

    @Override
    public void run() {
      LinearLayout.LayoutParams lin;
      lin = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
      long time = System.currentTimeMillis();
      int index = 0;
      for (Map.Entry<String, HudSpec> entry : hudMap.entrySet()) {
        final HudSpec spec = entry.getValue();
        //        hud.setClipChildren(false);
        final TextView text;
        if (index < hud.getChildCount()) {
          text = (TextView)hud.getChildAt(index);
        } else {
          text = new TextView(activity);
          final int topPad;
          if (spec == EMPTY_HUD_SPEC) {
            topPad = 5;
          } else {
            topPad = 0;
          }
          text.setTextSize(11.5f);
          text.setLineSpacing(0, 0.8f);
          text.setTextColor(Color.WHITE);
          text.setInputType(InputType.TYPE_NULL);
          text.setShadowLayer(2, 1, 1, Color.BLACK);
          text.setPadding(5,topPad,0,0);
          hud.addView(text, lin);
        }

        final String valueText;
        final String name = entry.getKey();
        if (spec == EMPTY_HUD_SPEC) {
          valueText = name;
        } else {
          final int seconds;
          if (spec.time > 0) {
            seconds = (int)((time - spec.time) / 1000);
          } else {
            seconds = Integer.MAX_VALUE;
          }
          if (seconds == Integer.MAX_VALUE) {
            valueText = String.format(Locale.US, name + ": %+.2f (-)", spec.value);
          } else if (Math.abs(seconds) < 10) {
            valueText = String.format(Locale.US, name + ": %+.2f", spec.value);
          } else {
            int clampSeconds = Math.max(-100, Math.min(100, seconds));
            valueText = String.format(Locale.US, name + ": %+.2f (%d)", spec.value, clampSeconds);
          }
        }
        if (!valueText.equals(text.getText())) {
          text.setText(valueText);
        }

        ++index;
      }

      clearRunnables();
    }

    private void clearRunnables() {
      if (hud == cameraHud) {
        cameraHudRunnable = null;
      }
      if (hud == previewHud) {
        previewHudRunnable = null;
      }
    }

  }

  private RelativeLayout previewStack; 
  private RelativeLayout cameraStack; 
  private LinearLayout previewHud; 
  private LinearLayout cameraHud; 
  private FrameLayout previewFrame;
  public FrameLayout createCameraFrame(boolean preview) {
    FrameLayout view;
    RelativeLayout stack;

    if (preview) {
      view = previewFrame;
      stack = previewStack;
    } else {
      view = cameraFrame;
      stack = cameraStack;
    }

    if (stack == null) {
      view = new FrameLayout(activity);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
          LayoutParams.MATCH_PARENT, CAMERA_WEIGHT);
      LinearLayout hud = new LinearLayout(activity);
      hud.setClipChildren(false);
      stack = createCameraView(view, hud);
      contentRow.addView(stack, 0, params);

      if (preview) {
        previewFrame = view;
        previewStack = stack;
        previewHud = hud;
      } else {
        cameraFrame = view;
        cameraStack = stack;
        cameraHud = hud;
      }

      dataEngine.listen(DataId.MODEL, new HudModelListener(hud));
    } else {
      if (stack.getVisibility() != View.VISIBLE) {
        stack.setVisibility(View.VISIBLE);
      }
    }
    
    if (preview) {
      updateViewButton(Actions.ACTION_PREVIEW.ordinal(), true);
    } else {
      updateViewButton(Actions.ACTION_CAMERA.ordinal(), true);
    }

    return view;
  }

  private Calendar calendar = GregorianCalendar.getInstance();

  private final class HudModelListener extends DataListener {
    private long time;
    private final LinearLayout hud;

    public HudModelListener(LinearLayout hud) {
      this.hud = hud;
    }

    @Override
    public void onDataChange(String id, ByteData data) {
      long now = System.currentTimeMillis();
      int intNow = (int)(now % Integer.MAX_VALUE);
      int intTime = ByteUtil.getInt(DataId.TIME, data.bytes);
      long timeDiff = intNow - intTime;

      final int max2 = Integer.MAX_VALUE / 2; 
      if (timeDiff > max2) {
        timeDiff = max2 - timeDiff;
      } else if (timeDiff < -max2) {
        timeDiff = (-max2) - timeDiff;
      }
      if (intTime == 0) {
        time = 0;
      } else {
        time = now - timeDiff;
      }

      Date date = new Date();
      calendar.setTime(date); 
      int timeHour = calendar.get(Calendar.HOUR_OF_DAY);
      int timeMinute = calendar.get(Calendar.MINUTE);
      int timeSecond = calendar.get(Calendar.SECOND);
      int timeMs = calendar.get(Calendar.MILLISECOND);
      //      float floatTime = timeHour + (timeMinute / 60f) + (timeSecond / (60 * 60f));
      float floatTime = 
          timeHour * 100 + 
          timeMinute * 1f + 
          timeSecond * .01f + 
          //          timeMs * 0.00001f + 
          0;
      setHudValue("Time", floatTime, now);
      setInt(data, "Mode", DataId.COMMAND);
      setInt(data, "Data", DataId.DATA);

      hudMap.put("Volt", EMPTY_HUD_SPEC);
      setFloat(data, "rxtx", DataId.MODEL_VOLT_MODEL);
      setFloat(data, "lipo", DataId.MODEL_VOLT_LIPO);
      setFloat(data, "bec", DataId.MODEL_VOLT_BEC);
      setFloat(data, "sensor", DataId.MODEL_VOLT_AUX);

      hudMap.put("Gps", EMPTY_HUD_SPEC);
      setFloat(data, "lat", DataId.MODEL_LAT);
      setFloat(data, "lon", DataId.MODEL_LON);
      setFloat(data, "alt", DataId.MODEL_ALT);

      hudMap.put("Attitude", EMPTY_HUD_SPEC);
      setFloat(data, "x>", DataId.GOAL_UP_X);
      setFloat(data, "x<", DataId.MODEL_UP_X);
      setFloat(data, "y>", DataId.GOAL_UP_Y);
      setFloat(data, "y<", DataId.MODEL_UP_Y);
      setFloat(data, "z>", DataId.GOAL_UP_Z);
      setFloat(data, "z<", DataId.MODEL_UP_Z);

      hudMap.put("Pwm", EMPTY_HUD_SPEC);
      setFloat(data, "ail", DataId.MODEL_PWM_AILERON);
      setFloat(data, "ele", DataId.MODEL_PWM_ELEVON);
      setFloat(data, "thr", DataId.MODEL_PWM_THROTTLE);
      setFloat(data, "rud", DataId.MODEL_PWM_RUDDER);
      setFloat(data, "aux", DataId.MODEL_PWM_AUX);

      updateHud();
    }

    private void setInt(ByteData data, String id, int offset) {
      setHudValue(id, (float)ByteUtil.getInt(offset, data.bytes), time);
    }
    private void setFloat(ByteData data, String id, int offset) {
      setHudValue(id, ByteUtil.getFloat(offset, data.bytes), time);
    }
  }



  private RelativeLayout createServoSliders(String id, int offset) {
    return createServoSliders(id, offset, 4, 8, 4);
  }
  private RelativeLayout createServoSliders(String id, int offset, int aileronOffset, int elevatorOffset, int rudderOffset) {
    final int max = 1023;
    RelativeLayout row = new RelativeLayout(activity);
    row.setGravity(Gravity.CENTER);
    int pad = 10;
    int sliderOffset = 10;

    SeekBar starBar;
    SeekBar rollBar;
    SeekBar portBar;
    SeekBar yawBar;
    RelativeLayout.LayoutParams  rel;

    starBar = createSlider(max, pad, -sliderOffset, max/2, false, true);
    dataEngine.listen(id, new ServoListener(max, starBar, offset + aileronOffset));
    rel = new RelativeLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    rel.addRule(RelativeLayout.CENTER_HORIZONTAL);
    row.addView(starBar, rel);
    //    addChild(row, starBar, WRAP_CONTENT, MATCH_PARENT, 0, ADD_NO_INDEX);

    rollBar = createSlider(max, pad, 0, max/2, false, true);
    dataEngine.listen(id, new ServoListener(max, rollBar, offset + elevatorOffset));
    rel = new RelativeLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    rel.addRule(RelativeLayout.CENTER_HORIZONTAL);
    row.addView(rollBar, rel);
    //    addChild(row, rollBar, WRAP_CONTENT, MATCH_PARENT, 0, ADD_NO_INDEX);

    portBar = createSlider(max, pad, sliderOffset, max/2, false, true);
    dataEngine.listen(id, new ServoListener(-max, portBar, offset + aileronOffset));
    rel = new RelativeLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    rel.addRule(RelativeLayout.CENTER_HORIZONTAL);
    row.addView(portBar, rel);
    //    addChild(row, portBar, WRAP_CONTENT, MATCH_PARENT, 0, ADD_NO_INDEX);

    yawBar = createSlider(max, 0, 0, max/2, false, false);
    dataEngine.listen(id, new ServoListener(-max, yawBar, offset + rudderOffset));
    rel = new RelativeLayout.LayoutParams(80, WRAP_CONTENT);
    rel.addRule(RelativeLayout.CENTER_VERTICAL);
    //    rel.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    //    rel.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    row.addView(yawBar, rel);
    return row;
  }

  private SeekBar createSlider(int max, int pad, int offset, int progress, boolean enabled, boolean vertical) {
    SeekBar slider;
    if (vertical) {
      slider = new VerticalSeekBar(activity);
    } else {
      slider = new SeekBar(activity);
    }
    slider.setKeyProgressIncrement(5);
    slider.setMax(max);
    slider.setProgress(progress);
    slider.setEnabled(enabled);
    if (vertical) {
      slider.setPadding(slider.getPaddingLeft(), pad + offset, slider.getPaddingRight(), pad - offset);
    } else {
      //      slider.setPadding(pad + offset, slider.getPaddingTop(), pad - offset, slider.getPaddingBottom());
    }
    return slider;
  }

  public View createThrottle() {
    LinearLayout column = new LinearLayout(activity);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

    //    LinearLayout row = new LinearLayout(context);
    //    row.setOrientation(LinearLayout.HORIZONTAL);

    RelativeLayout servoSliders; 
    servoSliders = createServoSliders(DataId.SENSOR, 0);
    addChild(column, servoSliders, WRAP_CONTENT, 0, 1f, ADD_END);
    servoSliders = createServoSliders(DataId.CONTROL, DataId.GOAL_UP_X);
    addChild(column, servoSliders, WRAP_CONTENT, 0, 1f, ADD_END);
    servoSliders = createServoSliders(DataId.MODEL, DataId.MODEL_PWM_AILERON, 0, 4, 12);
    addChild(column, servoSliders, WRAP_CONTENT, 0, 1f, ADD_END);

    
    final float inc = 0.1f;
    ButtonSpec upSpec = new ButtonSpec("+", 2, Color.GREEN, Actions.ACTION_UP);
    Button upButton = createButton(upSpec);
    upButton.setTextSize(labelScale);
//    upButton.setOnClickListener(new DataClickListener(DataId.SENSOR, THROTTLE_OFFSET, inc, false));
    upButton.setOnClickListener(new ThrottleClickListener(dataEngine, inc, true));
    column.addView(upButton);

    
    ButtonSpec downSpec = new ButtonSpec("-", 2, Color.RED, Actions.ACTION_DOWN);
    Button downButton = createButton(downSpec);
    downButton.setTextSize(labelScale);
//    downButton.setOnClickListener(new DataClickListener(DataId.SENSOR, THROTTLE_OFFSET, -inc, false));
    downButton.setOnClickListener(new ThrottleClickListener(dataEngine, -inc, true));
    column.addView(downButton);


    RelativeLayout row = new RelativeLayout(activity);
    int pad = 15;
    int sliderOffset = 10;

    modelThrottle = new VerticalSeekBar(activity); //, null, android.R.attr.progressBarStyleHorizontal);
    modelThrottle.setMax(THROTTLE_MAX);
    modelThrottle.setEnabled(false);
    modelThrottle.setPadding(modelThrottle.getPaddingLeft(), pad - sliderOffset, modelThrottle.getPaddingRight(), pad + sliderOffset);
    //    dataEngine.listen(DataId.MODEL, new ThrottleListener(modelThrottle));
    //    dataEngine.listen(DataId.SENSOR, new ThrottleDataListener(max, modelThrottle, 0));
    dataEngine.listen(DataId.MODEL, new ServoListener(THROTTLE_MAX, modelThrottle, DataId.MODEL_PWM_THROTTLE));
    addChild(row, modelThrottle, WRAP_CONTENT, MATCH_PARENT, 0, ADD_NO_INDEX);

    sensorThrottle = new VerticalSeekBar(activity); //, null, android.R.attr.progressBarStyleHorizontal);
    sensorThrottle.setMax(THROTTLE_MAX);
    sensorThrottle.setPadding(modelThrottle.getPaddingLeft(), pad, modelThrottle.getPaddingRight(), pad);


    sensorThrottle.setOnSeekBarChangeListener(
        //        new ThrottleBarListener(THROTTLE_MAX)
        new CommandBarListener(dataEngine, null, new SetupSpec("throttle", TYPE_INT, DataId.COMMAND_THROTTLE, 0, 1023, 1024))
        );

    


    //    dataEngine.listen(DataId.CONTROL, new ThrottleListener(sensorThrottle));
    //    dataEngine.listen(DataId.SENSOR, new ThrottleDataListener(max, sensorThrottle, 4));
    addChild(row, sensorThrottle, WRAP_CONTENT, MATCH_PARENT, 0, ADD_NO_INDEX);

    controlThrottle = new VerticalSeekBar(activity);
    controlThrottle.setKeyProgressIncrement(5);
    controlThrottle.setMax(THROTTLE_MAX);
    controlThrottle.setEnabled(false);
    controlThrottle.setPadding(controlThrottle.getPaddingLeft(), pad + sliderOffset, controlThrottle.getPaddingRight(), pad - sliderOffset);
    //    dataEngine.listen(DataId.SENSOR, new ServoListener(max, controlThrottle, THROTTLE_OFFSET));
    dataEngine.listen(DataId.CONTROL, new ThrottleListener(controlThrottle));
    addChild(row, controlThrottle, WRAP_CONTENT, MATCH_PARENT, 0, ADD_NO_INDEX);

    //    addChild(column, new View(context), MATCH_PARENT, pad, 0, ADD_END);
    addChild(column, row, WRAP_CONTENT, 0, 3f, ADD_END);
    //    addChild(column, new View(context), MATCH_PARENT, pad, 0, ADD_END);

    return column;
  }

  public static final float THIN_LINE_SPACING = 0.8f;
  public static final int ADD_END = -1;
  public static final int ADD_NO_INDEX = -2;
  private static float labelScale = 16f;
  public static final int THROTTLE_MAX = 1023;
  public static final int TRIM_CENTER = THROTTLE_MAX / 2;
  public static void addChild(
      ViewGroup parent, 
      View child, 
      int width, 
      int height, 
      float weight,
      int index) {
    int addIndex;
    if (index == ADD_END) {
      addIndex = parent.getChildCount();
    } else {
      addIndex = index;
    }
    ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
    child.setLayoutParams(params);

    if (index == ADD_NO_INDEX) {
      parent.addView(child);
    } else {
      parent.addView(child, addIndex);
    }
  }

  public void toggleVisible(Actions action) {
    if (action == Actions.ACTION_DISPLAY) {
      toggle(displayView, contentRow.indexOfChild(actionView), 0);
    } else if (action == Actions.ACTION_THROTTLE) {
      toggle(throttleView, 0, 0);
    } else if (action == Actions.ACTION_CAMERA) {
      toggle(cameraStack, 0, CAMERA_WEIGHT);
    } else if (action == Actions.ACTION_PREVIEW) {
      toggle(previewStack, 0, CAMERA_WEIGHT);
    } else if (action == Actions.ACTION_HUD) {
      toggle(previewHud, 0, CAMERA_WEIGHT);
      toggle(cameraHud, 0, CAMERA_WEIGHT);
    } else if (action == Actions.ACTION_LOG) {
    } else if (action == Actions.ACTION_HELP) {
    } else if (action == Actions.ACTION_SETUP) {
      toggle(setupView, 0, 0);
    }
  }

  private void toggle(View view, int index, int weight) {
    if (view.getParent() == null) {
      addUi(view, index, weight);
    } else {
      if (view.getVisibility() != View.VISIBLE) {
        view.setVisibility(View.VISIBLE);
      } else { 
        view.setVisibility(View.GONE);
      }
    }
  }



  private final class ThrottleBarListener implements SeekBar.OnSeekBarChangeListener {
    final int max;
    public ThrottleBarListener(int max) {
      this.max = max;
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
      if (fromUser) {
        //        AbcdActivity.appendStatus("onprog");
        //        final float throttle = convertFromProgress(progress, max);
        //        dataEngine.update(DataId.SENSOR, new FloatDataUpdate(THROTTLE_OFFSET, throttle, true));

        int[] values = {
            DataId.COMMAND_THROTTLE,
            progress,
        };
        ArrayDataUpdate update = new ArrayDataUpdate(DataId.CONTROL, DataId.COMMAND, values, true, DataId.TIME);
        update.doUpdate(dataEngine, System.currentTimeMillis());
      }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
  }

}
