package com.thomastechnics.abcd;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
public class CameraEngine extends SurfaceView implements SurfaceHolder.Callback {
  private SurfaceHolder mHolder;
  public Camera mCamera;
  private PreviewCallback mCallback;

  public CameraEngine(Context context, Camera camera, PreviewCallback callback) {
    super(context);
    AbcdActivity.appendStatus("CameraPreview");



//    initRecorder();


    mCamera = camera;
    mCallback = callback;

    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    mHolder = getHolder();
    mHolder.addCallback(this);
    // deprecated setting, but required on Android versions prior to 3.0
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

//  private void initRecorder() {
//    recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
//    recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
//
//    CamcorderProfile cpHigh = CamcorderProfile
//        .get(CamcorderProfile.QUALITY_HIGH);
//    recorder.setProfile(cpHigh);
//    recorder.setOutputFile("/sdcard/example.mp4");
//    recorder.setMaxDuration(50000); // 50 seconds
//    recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
//  }
//
//  private void prepareRecorder() {
//    recorder.setPreviewDisplay(mHolder.getSurface());
//
//    try {
//      recorder.prepare();
//    } catch (IllegalStateException e) {
//      e.printStackTrace();
//      //        finish();
//    } catch (IOException e) {
//      e.printStackTrace();
//      //        finish();
//    }
//  }

  boolean recording;
  public void setRecordingV2(boolean setRecording) {
    if (recording != setRecording) {
      if (!recording) {
        recording = prepareVideoRecorder();
        if (recording) {
          recorder.start();
          fixPreviewCallback();
        }
      } else {
        recorder.stop();
        releaseMediaRecorder();
        recording = false;
        fixPreviewCallback();
        
        updateGallery();
      }
    }
  }
  private void updateGallery() {
    getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
  }
  private void fixPreviewCallback() {
    mCamera.setPreviewCallbackWithBuffer(mCallback);
  }
  private boolean prepareVideoRecorder(){

//    mCamera = getCameraInstance();
    recorder = new MediaRecorder();

    // Step 1: Unlock and set camera to MediaRecorder
    mCamera.unlock();
    recorder.setCamera(mCamera);

    // Step 2: Set sources
    recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
    recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    
    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
    recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

    // other stuff?
//    recorder.setVideoSize(720, 480);
//    recorder.setVideoFrameRate(61);


    // Step 4: Set output file
    recorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

    // Step 5: Set the preview output
    recorder.setPreviewDisplay(mHolder.getSurface());

    // Step 6: Prepare configured MediaRecorder
    try {
        recorder.prepare();
    } catch (IllegalStateException e) {
      AbcdActivity.appendStatus("IllegalStateException preparing MediaRecorder: " + e.getMessage());
        releaseMediaRecorder();
        return false;
    } catch (IOException e) {
      AbcdActivity.appendStatus("IOException preparing MediaRecorder: " + e.getMessage());
        releaseMediaRecorder();
        return false;
    }
    return true;
}
  
  private void releaseMediaRecorder(){
    if (recorder != null) {
      recorder.reset();   // clear recorder configuration
      recorder.release(); // release the recorder object
      recorder = null;
      mCamera.lock();           // lock camera for later use
//      addBuffers(mCamera, Constants.PREVIEW_BUFFER_COUNT);
    }
  }

  public void surfaceCreated(SurfaceHolder holder) {
    AbcdActivity.appendStatus("CameraPreview surfaceCreated");

    mHolder = holder;
    
    // The Surface has been created, now tell the camera where to draw the preview.
    try {
      mCamera.setPreviewDisplay(holder);
      mCamera.startPreview();
//      prepareRecorder();
    } catch (IOException e) {
      // Log.d(TAG, "Error setting camera preview: " + e.getMessage());
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    AbcdActivity.appendStatus("CameraPreview surfaceDestroyed");

//    if (recording) {
//      recorder.stop();
//      recording = false;
//    }
//    recorder.release();

    // empty. Take care of releasing the Camera preview in your activity.
    // stop preview before making changes
    try {
      //        mCamera.stopPreview();
      //        mCamera.release();
    } catch (Exception e){
      //        // ignore: tried to stop a non-existent preview
    }
  }

  private int sizeIndex;
  private int maxSizeIndex = Integer.MAX_VALUE - 1;
  public void setSize(int sizeIndex, boolean update) {
    sizeIndex = Math.max(0, Math.min(maxSizeIndex, sizeIndex));
    if (this.sizeIndex != sizeIndex) {
      this.sizeIndex = sizeIndex;
      if (update) {
        this.surfaceChanged(mHolder, 0, 0, 0);
      }
    }
  }
  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    AbcdActivity.appendStatus("CameraPreview surfaceChanged");

//    mHolder = holder;
    
    // If your preview can change or rotate, take care of those events here.
    // Make sure to stop the preview before resizing or reformatting it.

    if (mHolder.getSurface() == null){
      // preview surface does not exist
      return;
    }

    // stop preview before making changes
    try {
      mCamera.stopPreview();
    } catch (Exception e){
      // ignore: tried to stop a non-existent preview
    }

    // set preview size and make any resize, rotate or
    // reformatting changes here

    // start preview with new settings
    try {
      setParameters();

      mCamera.setPreviewDisplay(mHolder);
      requestLayout();

//      if (!calledback) {
//        calledback = true;
      mCamera.setPreviewCallbackWithBuffer(mCallback);
//      }

      // hmm, maybe 3 need to be used at the same time?
      // 1 to be sit in the preview data
      // 1 to send in the previewFrame call
      // 1 to be writing the next frame to in the camera
      addBuffers(mCamera, Constants.PREVIEW_BUFFER_COUNT);

      mCamera.startPreview();

    } catch (Exception e){
      AbcdActivity.appendStatus("Error starting camera preview: " + e.getMessage());
    }
  }
  private void setParameters() {
    //          AbcdActivity.setupCamera(mCamera);
    AbcdActivity.appendStatus("setupCamera begin");
    Camera.Parameters param = mCamera.getParameters();

    param.setRecordingHint(true);
    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    
    if (param.getMaxNumFocusAreas() > 0) {
      List<Camera.Area> areaList = new ArrayList<Camera.Area>();
      // areaList.add(new Camera.Area(new Rect(-1000, -1000, 1000, 1000), 1));
      int goldenB = (int)(1000 / (1.618f * 1.618f));
      areaList.add(new Camera.Area(new Rect(-goldenB, -goldenB, goldenB, goldenB), 1));
      param.setFocusAreas(areaList);
    }
    
    String focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
    if (param.getSupportedFocusModes().contains(focusMode)) {
      AbcdActivity.appendStatus("setFocusMode " + focusMode);
      param.setFocusMode(focusMode);
    }

    //          param.setPreviewFormat(ImageFormat.NV21);

    int[] doubleZero = {0,0};
    int[] maxFps = doubleZero;
    for (int[] fps : param.getSupportedPreviewFpsRange()) {
      if ((fps[0] > maxFps[0]) ||
          ((fps[0] == maxFps[0]) && fps[1] > maxFps[1])) 
      {
        maxFps = fps.clone();
      }
    }
    if (maxFps != doubleZero) {
      param.setPreviewFpsRange(maxFps[0], maxFps[1]);
    }



    // no GN
    // int x = 176;
    // int y = 144;

    // no NS
    int x = 240;
    int y = 160;

    // no GN, NS
    // int x = 320;
    // int y = 240;

    // all good
    // int x = 640;
    // int y = 480;

    //          param.setPictureSize(640, 480);
    //          param.setPreviewSize(x, y);

    List<Size> supported;
    int[] sizeWH;
    int[] idealSize;

//      idealSize = new int[] {640,480};
//      supported =  param.getSupportedPictureSizes();
//      sizeWH = calcIdealSize(doubleZero, idealSize, supported);
//      if (sizeWH != doubleZero) {
//        //            AbcdActivity.appendStatus("setPictureSize");
//        param.setPictureSize(sizeWH[0], sizeWH[1]);
//      }

    idealSize = new int[] {720,480};
    supported =  param.getSupportedVideoSizes();
    sizeWH = calcIdealSize(doubleZero, idealSize, supported);
    if (sizeWH != doubleZero) {
      //            AbcdActivity.appendStatus("setPictureSize");
      param.setPictureSize(sizeWH[0], sizeWH[1]);
    }

    idealSize = new int[] {x, y};
    supported =  param.getSupportedPreviewSizes();
    //          sizeWH = calcIdealSize(doubleZero, idealSize, supported);
    maxSizeIndex = supported.size() - 1;
    sizeIndex = Math.max(0, Math.min(maxSizeIndex, sizeIndex));
    int scaledIndex = maxSizeIndex - sizeIndex;
    Size size = supported.get(scaledIndex);
    sizeWH = new int[] {
        size.width, 
        size.height,
    };
    if (sizeWH != doubleZero) {
      //            AbcdActivity.appendStatus("setPreviewSize");
      param.setPreviewSize(sizeWH[0], sizeWH[1]);
    }

    AbcdActivity.appendStatus("setupCamera " + param.flatten());
    mCamera.setParameters(param);
    AbcdActivity.appendStatus("setupCamera ended");
  }
  
//  boolean calledback;

  private int[] calcIdealSize(int[] zero, int[] ideal, List<Size> supported) {
    int[] sizeWH = zero;
    float minSizeDiff = calcDiff(ideal, sizeWH[0], sizeWH[1]);
    for (Size size : supported) {
      float diff = calcDiff(ideal, size.width, size.height);
      //        AbcdActivity.appendStatus("calcIdealSize w " + size.width + " h " + size.height);
      if (diff < minSizeDiff) {
        //          AbcdActivity.appendStatus("calcIdealSize diff " + diff + " min " + minSizeDiff);
        minSizeDiff = diff;
        sizeWH = new int[] {
            size.width,
            size.height,
        };
      }
    }
    return sizeWH;
  }

  private float calcDiff(int[] idealSize, int width, int height) {
    int[] diffSize = {
        width - idealSize[0],
        height - idealSize[1],
    };
    float diff = (float)Math.sqrt(diffSize[0] * diffSize[0] + diffSize[1] * diffSize[1]);
    return diff;
  }

  public static int calcByteCount(Camera.Parameters param) {
    final int format = param.getPreviewFormat();
     final Camera.Size size = param.getPreviewSize();
//    final Camera.Size size = param.getPictureSize();

    final int pixelBits = ImageFormat.getBitsPerPixel(format);
    final int pixelCount = size.width * size.height;
//    final int pixelCount = 1920 * 1080;

    final int byteBits = 8;
    final int byteCount = (pixelCount * pixelBits) / byteBits;

    return byteCount;
  }

  public static void addBuffers(Camera camera, int bufferCount) {
    final Camera.Parameters param = camera.getParameters();
    final int byteCount = calcByteCount(param);

    for (int bufferIndex = 0; bufferIndex < bufferCount; bufferIndex++) {
      byte[] buffer = new byte[byteCount];
      camera.addCallbackBuffer(buffer);
    }
  }

  /** A safe way to get an instance of the Camera object. */
  public static Camera getCameraInstance(){
    Camera c = null;
    try {
      c = Camera.open(); // attempt to get a Camera instance
    }
    catch (Exception e){
      // Camera is not available (in use or does not exist)
    }
    return c; // returns null if camera is unavailable
  }

  // Method from Ketai project! Not mine! See below...
  public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

    final int frameSize = width * height;

    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
      for (int i = 0; i < width; i++, yp++) {
        int y = (0xff & ((int) yuv420sp[yp])) - 16;
        if (y < 0)
          y = 0;
        if ((i & 1) == 0) {
          v = (0xff & yuv420sp[uvp++]) - 128;
          u = (0xff & yuv420sp[uvp++]) - 128;
        }

        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        if (r < 0)
          r = 0;
        else if (r > 262143)
          r = 262143;
        if (g < 0)
          g = 0;
        else if (g > 262143)
          g = 262143;
        if (b < 0)
          b = 0;
        else if (b > 262143)
          b = 262143;

        rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
      }
    }
  }


  public void updateRec(final boolean rec)
  {
//    synchronized (this) {
//      boolean isRec = (recorder != null); 
//      if (rec != isRec) {
//        if (rec) {
//          try {
//            startRecording();
//          } catch (Exception e) {
//            String message = e.getMessage();
//            AbcdActivity.appendStatus("updateRec " + message);
//            //              Log.i(null, "Problem Start"+message);
//            recorder.release();
//          }
//        } else {
//          recorder.stop();
//          recorder.release();
//          recorder = null;
//        }
//      }
//  }

    post(new Runnable() {
      @Override
      public void run() {
        setRecordingV2(rec);
      }
    });
  }

  private MediaRecorder recorder;
//  protected void startRecording() throws IOException 
//  {
//    recorder = new MediaRecorder();  // Works well
//    //    mCamera.unlock();
//
//    recorder.setCamera(mCamera);
//    recorder.setPreviewDisplay(mHolder.getSurface());
//
//    //    mrec.setPreviewDisplay(surfaceHolder.getSurface());
//    recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
//    recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT); 
//
//    CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//    recorder.setProfile(cpHigh);
//    //    recorder.setOutputFile("/sdcard/videocapture_example.mp4");
//    recorder.setMaxDuration(50000); // 50 seconds
//    recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
//
//    //    mrec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//    //    mrec.setPreviewDisplay(surfaceHolder.getSurface());
//    recorder.setOutputFile("/sdcard/" + System.currentTimeMillis() + ".3gp"); 
//
//    recorder.prepare();
//    recorder.start();
//  }

//  protected void stopRecording() {
//    recorder.stop();
//    recorder.release();
//    mCamera.release();
//  }

//  private void releaseMediaRecorder(){
//    if (recorder != null) {
//      recorder.reset();   // clear recorder configuration
//      recorder.release(); // release the recorder object
//      recorder = null;
//      mCamera.lock();           // lock camera for later use
//    }
//  }

  public static final int MEDIA_TYPE_IMAGE = 1;
  public static final int MEDIA_TYPE_VIDEO = 2;

  /** Create a file Uri for saving an image or video */
  private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
  }

  /** Create a File for saving an image or video */
  private static File getOutputMediaFile(int type){
      // To be safe, you should check that the SDCard is mounted
      // using Environment.getExternalStorageState() before doing this.

      File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Abcd");
      // This location works best if you want the created images to be shared
      // between applications and persist after your app has been uninstalled.

      // Create the storage directory if it does not exist
      if (! mediaStorageDir.exists()){
          if (! mediaStorageDir.mkdirs()){
//              Log.d("MyCameraApp", "failed to create directory");
              return null;
          }
      }

      // Create a media file name
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      File mediaFile;
      if (type == MEDIA_TYPE_IMAGE){
          mediaFile = new File(mediaStorageDir.getPath() + File.separator +
          "IMG_"+ timeStamp + ".jpg");
      } else if(type == MEDIA_TYPE_VIDEO) {
          mediaFile = new File(mediaStorageDir.getPath() + File.separator +
          "VID_"+ timeStamp + ".mp4");
      } else {
          return null;
      }

      return mediaFile;
  }
}