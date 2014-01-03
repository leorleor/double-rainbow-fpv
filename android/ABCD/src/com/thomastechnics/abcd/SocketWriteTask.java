package com.thomastechnics.abcd;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


public abstract class SocketWriteTask extends SocketTask {

  protected final DataEngine dataEngine;
  protected final String outputId;
  protected final Socket socket;
  protected final ByteData outputData;
  protected final ByteData outputBuffer;

  public SocketWriteTask(DataEngine dataEngine, String outputId, Socket socket) {
    this.socket = socket;
    this.dataEngine = dataEngine;
    this.outputId = outputId;
    this.outputData = dataEngine.get(outputId);
    outputBuffer = new ByteData();
    
    AbcdActivity.appendStatus("SocketWriteTask " + outputData.name);
  }

  protected void write() throws IOException {
//    AbcdActivity.appendStatus("write " + outputData.name);
    if (outputData.length > 0) {
      outputData.copyTo(outputBuffer);
      final OutputStream outputStream = socket.getOutputStream();
      outputStream.write(outputBuffer.bytes, 0, outputBuffer.getLength());
      outputStream.flush();
      
//      if (outputId == DataId.CAMERA) {
//        AbcdActivity.appendStatus("outputBuffer " + outputId + " " 
//            + " " + outputBuffer.getLength()
//            + " " + ByteUtil.getInt(0 * ByteUtil.INT_SIZE, outputBuffer.bytes)
//            + " " + ByteUtil.getInt(1 * ByteUtil.INT_SIZE, outputBuffer.bytes)
//            + " " + ByteUtil.getInt(2 * ByteUtil.INT_SIZE, outputBuffer.bytes)
//            + " " + ByteUtil.getInt(3 * ByteUtil.INT_SIZE, outputBuffer.bytes)
//          );
//      }
    }
  }
}