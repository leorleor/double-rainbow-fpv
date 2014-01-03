package com.thomastechnics.abcd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketStarter {
  /**
   * Traffic class does not matter much, but might as well. Set to reliability,
   * throughput, and low delay.
   * 
   * http://docs.oracle.com/javase/6/docs/api/java/net/DatagramSocket.html
   * 
   * for Internet Protocol v4 the value consists of an octet with precedence and
   * TOS fields as detailed in RFC 1349. The TOS field is bitset created by
   * bitwise-or'ing values such the following :-
   * 
   * IPTOS_LOWCOST (0x02)
   * 
   * IPTOS_RELIABILITY (0x04)
   * 
   * IPTOS_THROUGHPUT (0x08)
   * 
   * IPTOS_LOWDELAY (0x10)
   */
  private static final int TRAFFIC_CLASS = ((0x04) | (0x08) | (0x10));

  private SocketTask task;
  private ConnectListener listener;

  public SocketStarter() {
  }

  public void setListener(ConnectListener listener) {
    this.listener = listener;
  }

  public void start(boolean listen, String host, int port) {
    final SocketTask newTask;

    if (listen) {
      newTask = new ListenTask(listener, port);
    } else {
      newTask = new ConnectTask(listener, host, port);
    }

    setTask(newTask);
  }

  public void stop() {
    setTask(null);
  }

  public void setTask(SocketTask newTask) {
    synchronized (this) {
      if (task != null) {
        task.stop();
      }

      task = newTask;

      if (task != null) {
        task.start();
      }
    }
  }

  public static class ConnectListener {
    public void onListen(Socket socket) {
    }

    public void onConnect(Socket socket) {
    }
  }

  private static final int BUFFER_SIZE = 64 * 1024;

  private static class ListenTask extends SocketTask {
    final private ConnectListener listener;
    final private int port;
    private ServerSocket serverSocket;

    public ListenTask(ConnectListener listener, int port) {
      this.listener = listener;
      this.port = port;
    }

    @Override
    public void stop() {
      super.stop();

      AbcdActivity.appendStatus("ListenTask close");
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          AbcdActivity.appendStatus("ListenTask close " + e);
        }
      }
    }

    protected void runSocket() {
      AbcdActivity.appendStatus("ListenTask runSocket");
      Socket socket = null;

      try {
        if (serverSocket == null) {
          serverSocket = new ServerSocket();

          serverSocket.setReceiveBufferSize(BUFFER_SIZE);
          // serverSocket.setReuseAddress(true);
          serverSocket.setSoTimeout(Constants.LISTEN_TIMEOUT);
          serverSocket.setPerformancePreferences(0, 2, 1);

          serverSocket.bind(new InetSocketAddress(port));
        }

        socket = serverSocket.accept();

        listener.onListen(socket);
      } catch (IOException e) {
        // AbcdActivity.appendStatus("ListenTask accept " + e);
      }
    }
  }

  private static class ConnectTask extends SocketTask {
    final private ConnectListener listener;
    final private String host;
    final private int port;

    public ConnectTask(ConnectListener listener, String host, int port) {
      this.listener = listener;
      this.host = host;
      this.port = port;
    }

    @Override
    protected void runSocket() {
//      AbcdActivity.appendStatus("ConnectTask runSocket");
      Socket socket = null;

      try {
        socket = new Socket();

        socket.setTcpNoDelay(true);
        // socket.setKeepAlive(false);
        socket.setSendBufferSize(256 * 1024);
        socket.setReceiveBufferSize(256 * 1024);
        // socket.setReuseAddress(true);
        socket.setSoTimeout(Constants.READ_TIMEOUT);
        // socket.setSoLinger(true, CLOSE_TIMEOUT);
        socket.setSoLinger(false, 0);
        socket.setTrafficClass(TRAFFIC_CLASS);
        socket.setPerformancePreferences(0, 2, 1);

        socket.connect(new InetSocketAddress(host, port), Constants.CONNECT_TIMEOUT);

        listener.onConnect(socket);
      } catch (IOException e) {
      }
    }
  }
}
