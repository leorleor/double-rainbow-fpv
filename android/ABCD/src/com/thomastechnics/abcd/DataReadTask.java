package com.thomastechnics.abcd;

import java.io.IOException;

public class DataReadTask extends SocketReadTask {
  private final DataSocket dataSocket;
  private long lastReadTime;
  
  public DataReadTask(DataSocket dataSocket) {
    super(dataSocket.getDataEngine(), dataSocket.getInputId(), dataSocket.getSocket());
    this.dataSocket = dataSocket;
  }

  @Override
  protected void runSocket() throws IOException {
//    long begin = System.currentTimeMillis();
    
    // Reads up to the max number of expected pending writes.
    // The limit is so that it does not get stuck reading continuously.
    for (int readCount = 0; readCount < Constants.MAX_READ_COUNT; ) {
      int readLength;
      try {
        readLength = read();
      } catch (IOException exception) {
        readLength = 0;
        dataSocket.onException(exception);
      }
      
      if (readLength > 0) {
        long inputTime = inputData.getTimestamp(); 
        if (inputTime > lastReadTime) {
          lastReadTime = inputTime;
          
//          long ended = System.currentTimeMillis();
//          AbcdActivity.appendStatus("read time " + dataSocket.getInputData().name + " " + dataSocket.getOutputData().name + " " + (ended - begin));
//          begin = System.currentTimeMillis();
          
          dataSocket.onDataRead();
          ++readCount;
        }
      } else {
        // Skips reading when data is not available.
        break;
      }
    }
  }
}