package com.thomastechnics.abcd;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;


public abstract class SocketReadTask extends SocketTask {

  protected final ByteData inputData;
  protected final DataEngine dataEngine;
  protected final String inputId;
  protected final Socket socket;
  protected final ByteData inputBuffer;
  private int inputOffset;
  boolean needHeader = false;
  int headerOffset;

  public SocketReadTask(DataEngine dataEngine, String inputId, Socket socket) {
    this.socket = socket;
    this.inputId = inputId;
    this.dataEngine = dataEngine;
    this.inputData = dataEngine.get(inputId);

    inputBuffer = new ByteData();
    inputBuffer.setLength(inputData.getLength());
    if (inputBuffer.length == 0) {
      needHeader = true;
      inputBuffer.setLength(VariableLengthData.HEADER_LENGTH);
    }
    
    AbcdActivity.appendStatus("SocketReadTask " + inputData.name);
  }
  
  protected int read() throws IOException {
//    AbcdActivity.appendStatus("read " + inputData.name);
    
    final InputStream inputStream = socket.getInputStream();
    final int readLength = Math.min(Constants.MAX_READ_LENGTH, inputBuffer.getLength()
        - inputOffset);
    
    int readCount = 0;
    if (inputStream.available() > readLength) {
      if (inputBuffer.bytes == null) {
        inputBuffer.bytes = null;
      }
      readCount = inputStream.read(inputBuffer.bytes, inputOffset, readLength);
    }
    
    if (readCount > 0) {
      inputOffset += readCount;
  
      if (inputOffset == inputBuffer.getLength()) {
        if (needHeader && (inputBuffer.getLength() == VariableLengthData.HEADER_LENGTH)) {
          int needLength = ByteUtil.getInt(0, inputBuffer.bytes);
          inputBuffer.setLength(needLength);
        }
        
        if (inputOffset >= inputBuffer.getLength()) {
          inputBuffer.setTimestamp(System.currentTimeMillis());
//          if (inputBuffer.timestamp > dataEngine.get(inputId).timestamp) {
            dataEngine.swapTo(inputBuffer, inputId);
//          }
          
          if (needHeader) {
            inputBuffer.setLength(VariableLengthData.HEADER_LENGTH);
          }
          
          inputOffset = 0;
        }
      }
    }
    
    return readCount;
  }

}