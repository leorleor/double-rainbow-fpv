package com.thomastechnics.abcd;

import java.io.IOException;
import java.net.Socket;

public class DataSession {
  private AbcdActivity activity;
  private boolean closed;
  
  public DataSession(AbcdActivity activity) {
    this.activity = activity;
  }

  public void onClose(Socket socket, IOException exception) {
    synchronized (this) {
      if (!closed) {
        closed = true;
        
        activity.reconnectNetwork();
      }
    }
  }
}
