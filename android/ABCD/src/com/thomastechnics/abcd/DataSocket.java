package com.thomastechnics.abcd;

import java.io.IOException;
import java.net.Socket;

public class DataSocket {
  private Socket socket;
  
  private int unrepliedWrites;
  private long lastWriteTime;
  private long lastReadTime;
  private long lastOutputDataTime;

  private DataEngine dataEngine;
  private String inputId;
  private String outputId;

  private ByteData inputData;
  private ByteData outputData;

  private DataWriteTask writeTask;

  private DataReadTask readTask;
  
  private DataSession session;

  public DataSocket() {
  }

  public void setListen(boolean listen) {
    if (listen) {
      // Waits for reads before writing.
      unrepliedWrites = Constants.MAX_UNREPLIED_WRITES;
    }
  }

  public DataEngine getDataEngine() {
    return dataEngine;
  }

  public void setDataEngine(DataEngine dataEngine) {
    this.dataEngine = dataEngine;
  }

  public String getInputId() {
    return inputId;
  }

  public void setInputId(String inputId) {
    this.inputId = inputId;
    setInputData(dataEngine.get(inputId));
  }

  public String getOutputId() {
    return outputId;
  }

  public void setOutputId(String outputId) {
    this.outputId = outputId;
    setOutputData(dataEngine.get(outputId));
  }

  public Socket getSocket() {
    return socket;
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }
  
  public ByteData getInputData() {
    return inputData;
  }

  public void setInputData(ByteData inputData) {
    this.inputData = inputData;
  }

  public ByteData getOutputData() {
    return outputData;
  }

  public void setOutputData(ByteData outputData) {
    this.outputData = outputData;
  }
  
  public void start() {
    synchronized (this) {
      long startTime = System.currentTimeMillis();
      lastWriteTime = startTime;
      lastReadTime = startTime;
      
      readTask = new DataReadTask(this);
      writeTask = new DataWriteTask(this);
      readTask.setRepeat(Constants.DATA_REPEAT);
      writeTask.setRepeat(Constants.DATA_REPEAT);
      readTask.start();
      writeTask.start();
    }
  }
  public void stop() {
    synchronized (this) {
      if (writeTask != null) {
        writeTask.stop();
        readTask.stop();
        writeTask = null;
        readTask = null;
      }
    }
  }
  
  public void onDataRead() {
//    AbcdActivity.appendStatus("onDataRead " + inputData.name + " " + outputData.name + " " + unrepliedWrites);
    synchronized (this) {
      unrepliedWrites = Math.max(0, unrepliedWrites - 1);
      lastReadTime = System.currentTimeMillis();
    }
  }
  
  public void onDataWrite() {
//    AbcdActivity.appendStatus("onDataWrite " + inputData.name + " " + outputData.name + " " + unrepliedWrites);
    synchronized (this) {
      unrepliedWrites = Math.min(Constants.MAX_UNREPLIED_WRITES, unrepliedWrites + 1);
      lastWriteTime = System.currentTimeMillis();
    }
  }
  
  public void onException(IOException exception) {
    synchronized (this) {
//      AbcdActivity.appendStatus("onException " + socket.isInputShutdown() + " " + socket.isOutputShutdown() + " " + socket.isClosed() + " " + (!socket.isConnected()) + " " + exception);
      //    if (socket.isInputShutdown() || socket.isOutputShutdown() || socket.isClosed() || (!socket.isConnected())) {
      long writeDelay = System.currentTimeMillis() - lastWriteTime;
      long readDelay = System.currentTimeMillis() - lastReadTime;

      if ((writeDelay > 10 * Constants.MAX_WRITE_DELAY) || (readDelay > 10 * Constants.MAX_WRITE_DELAY)) {
        AbcdActivity.appendStatus("onException " + writeDelay + " " + readDelay + " " + 10 * Constants.MAX_WRITE_DELAY);

        stop();

        try {
          socket.close();
        } catch (IOException e) {
          AbcdActivity.appendStatus("onException close " + e.toString());
        }

        session.onClose(socket, exception);
      }
    }
  }
  
  public int getUnrepliedWrites() {
    return unrepliedWrites;
  }

  public boolean shouldWriteNow() {
    boolean should = false;

    // Delays writing when reading stops.
    if (unrepliedWrites < Constants.MAX_UNREPLIED_WRITES) {
      // Only writes if there is new output.
      if (lastOutputDataTime < outputData.getTimestamp()) {
        should = true;
      }
    }
    
    if (!should) {
      // If it has been too long since the last write, 
      // then writes anyway.
      long delay = System.currentTimeMillis() - lastWriteTime;
      if (delay > Constants.MAX_WRITE_DELAY) {
        AbcdActivity.appendStatus("shouldWriteNow " + outputId + " delay " + delay);
        // unrepliedWrites = Math.max(0, unrepliedWrites - 1);
        should = true;
      }
    }
    
    return should;
  }

  public void setSession(DataSession session) {
    this.session = session;
  }
}
