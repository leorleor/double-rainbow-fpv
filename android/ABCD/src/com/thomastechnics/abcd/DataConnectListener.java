package com.thomastechnics.abcd;

import java.net.Socket;
import java.util.List;

import com.thomastechnics.abcd.SocketStarter.ConnectListener;

public class DataConnectListener extends ConnectListener {
  private int socketCount;
  private SocketStarter starter;
  public List<DataSocket> dataList;
  
  public DataConnectListener() {
  }
  
  public void setStarter(SocketStarter starter) {
    this.starter = starter;
  }
  public void setData(List<DataSocket> dataList) {
    this.dataList = dataList;
  }
  
  @Override
  public void onListen(Socket socket) {
    AbcdActivity.appendStatus("onListen");
    onSocket(socket);
  }
  
  @Override
  public void onConnect(Socket socket) {
    AbcdActivity.appendStatus("onConnect");
    onSocket(socket);
  }
  
  public void onSocket(Socket socket) {
    AbcdActivity.appendStatus("onSocket");
    DataSocket data = dataList.get(socketCount);
    data.setSocket(socket);
    ++socketCount;
    
    AbcdActivity.appendStatus("dataCount " + dataList.size() + " socketCount " + socketCount);
    if (dataList.size() == socketCount) {
      starter.stop();
      onCount();
    }
  }
  
  public void onCount() {
  }

  protected static void addData(boolean listen, List<DataSocket> dataList, 
//      ByteData inputData, ByteData outputData,
      DataEngine dataEngine,
      String inputId, String outputId,
      DataSession session) {
    DataSocket data = new DataSocket();
    data.setListen(listen);
    data.setSession(session);
//    data.setInputData(inputData);
//    data.setOutputData(outputData);
    data.setDataEngine(dataEngine);
    data.setInputId(inputId);
    data.setOutputId(outputId);
    dataList.add(data);
  }
}