package com.sonarsource.braindevday.weatherserver;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Main {

  private static final String PORT_NAME = "COM3";
  private static final int BAUD_RATE = 115200;

  private static Client client;

  public static void main(String[] args) throws Exception {

    Settings settings = ImmutableSettings.settingsBuilder()
        .put("cluster.name", "braindevday").put("node.name", "weatherserver")
        .build();
    client = new TransportClient(settings)
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

    CommPortIdentifier portIdentifier = CommPortIdentifier
        .getPortIdentifier(PORT_NAME);
    if (portIdentifier.isCurrentlyOwned()) {
      System.out.println("Error: Port is currently in use");
    } else {
      CommPort commPort = portIdentifier.open(Main.class.getName(), 2000);
      if (commPort instanceof SerialPort) {
        SerialPort serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(BAUD_RATE, SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        InputStream in = serialPort.getInputStream();
        (new Thread(new SerialReader(in))).start();
      } else {
        System.out
            .println("Error: Only serial ports are handled by this example.");
      }
    }

    while (true) {
    }
  }

  private static class SerialReader implements Runnable {
    InputStream in;

    public SerialReader(InputStream in) {
      this.in = in;
    }

    public void run() {
      SimpleDateFormat dateFormat = new SimpleDateFormat(
          "yyyy-MM-dd'T'hh:mm:ssZ");
      StringBuilder jsonBuilder = new StringBuilder();
      byte[] buffer = new byte[8];
      int len = -1;
      try {
        while ((len = this.in.read(buffer)) > -1) {
          String next = new String(buffer, 0, len);
          jsonBuilder.append(next);
          if (jsonBuilder.length() > 0
              && jsonBuilder.charAt(jsonBuilder.length() - 1) == '\n') {
            String[] jsonStrings = jsonBuilder.toString().split("\\n");
            for (String json : jsonStrings) {
              JSONObject obj = (JSONObject) JSONValue.parse(json);
              if (obj != null) {
                obj.put("timestamp", dateFormat.format(new Date()));
                client.prepareIndex().setIndex("weather").setType("measure")
                    .setSource(obj.toJSONString()).execute().actionGet();
              }
            }
            jsonBuilder = new StringBuilder();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
