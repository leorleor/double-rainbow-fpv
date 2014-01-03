package com.thomastechnics.abcd;

import java.io.IOException;

public class DataWriteTask extends SocketWriteTask {
  private final DataSocket dataSocket;
  
  public DataWriteTask(DataSocket dataSocket) {
    super(dataSocket.getDataEngine(), dataSocket.getOutputId(), dataSocket.getSocket());
    this.dataSocket = dataSocket;
  }

  @Override
  protected void runSocket() throws IOException {
    if (dataSocket.shouldWriteNow()) {
      try {
//        long begin = System.currentTimeMillis();
        write();
//        long ended = System.currentTimeMillis();
//        AbcdActivity.appendStatus("write time " + dataSocket.getInputData().name + " " + dataSocket.getOutputData().name + " " + (ended - begin));
        dataSocket.onDataWrite();
      } catch (IOException exception) {
        dataSocket.onException(exception);
      }
    }
  }
}