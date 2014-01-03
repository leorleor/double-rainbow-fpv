package com.thomastechnics.abcd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DataEngine {
  
  private Map<String, ByteData> dataMap = new Hashtable<String, ByteData>();
  private Map<String, List<DataListener>> listenMap = new HashMap<String, List<DataListener>>();
  
  public void add(String id, ByteData data) {
    dataMap.put(id, data);
    onChange(id);
  }
  public ByteData get(String id) {
    return dataMap.get(id);
  }
  public void listen(String id, DataListener listener) {
    List<DataListener> list;
    synchronized (listenMap) {
      list = listenMap.get(id);
      if (list == null) {
        list = Collections.synchronizedList(new LinkedList<DataListener>());
        listenMap.put(id, list);
      }
    }
    list.add(listener);
    listener.onDataChange(id, dataMap.get(id));
  }
  
  public void copy(byte[] bytes, int length, long timestamp, String id) {
    dataMap.get(id).copy(bytes, length, timestamp);
    onChange(id);
  }
  public byte[] set(byte[] bytes, int length, long timestamp, String id) {
    byte[] swapBytes = dataMap.get(id).set(bytes, length, timestamp);
    onChange(id);
    return swapBytes;
  }

  public void copyTo(ByteData fromData, String id) {
    fromData.copyTo(dataMap.get(id));
    onChange(id);
  }
  public void swapTo(ByteData fromData, String id) {
//    System.err.println("swapTo id(" + id + ") fromData("+fromData+")");
    fromData.swapTo(dataMap.get(id));
    onChange(id);
  }

  public ByteData newBuffer(String id) {
    final ByteData buffer = new ByteData(dataMap.get(id).getLength());
    return buffer;
  }

  public void update(String id, DataUpdate update) {
    ByteData data = dataMap.get(id);
    synchronized (data) {
      update.update(id, data);
    }
    onChange(id);
  }

  public static interface DataUpdate {
    public void update(String id, ByteData data);
  }

  private void onChange(String id) {
    List<DataListener> listenList = listenMap.get(id);
    ByteData data = dataMap.get(id);
    if (listenList != null) {
//      System.err.println("onChange id(" + id + ") fromData("+listenList.size()+")");
      for (DataListener listen : listenMap.get(id)) {
        listen.onDataChange(id, data);
      }
    }
  }
}
