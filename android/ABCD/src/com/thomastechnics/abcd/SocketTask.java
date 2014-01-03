package com.thomastechnics.abcd;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public abstract class SocketTask {
  private Timer timer;
  private boolean running;
  private boolean scheduled;
  private int repeat = Constants.SOCKET_REPEAT;
  
  public void start() {
    synchronized (this) {
      if (!running) {
        running = true;
        timer = new Timer();
        schedule(0);
      }
    }
  }

  public void stop() {
    synchronized (this) {
      if (running) {
        running = false;
        timer.cancel();
        timer.purge();
        timer = null;
      }
    }
  }
  
  public void setRepeat(int repeat) {
    this.repeat = repeat;
  }
  
  public void schedule(long delayMs) {
    synchronized(this) {
      if (running && !scheduled) {
        delayMs = Math.max(Constants.MIN_DELAY, delayMs);
        timer.schedule(new ScheduledRun(), delayMs);
        scheduled = true;
      }
    }
  }

  protected boolean repeat() {
    return true;
  }

  protected abstract void runSocket() throws IOException;

  
  private class ScheduledRun extends TimerTask {
    @Override
    public void run() {
      scheduled = false;
      
      long beginMs = System.currentTimeMillis();

      if (running) {
        try {
          runSocket();
        } catch (SocketTimeoutException e) {
        } catch (IOException e) {
//          AbcdActivity.appendStatus("runSocket " + e);
        }
      }
      
      long endMs = System.currentTimeMillis();

      if (running) {
        if (repeat()) {
          long runMs = endMs - beginMs;
          long delayMs = repeat - runMs;
          schedule(delayMs);
        }
      }
    }
 }
}