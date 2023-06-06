package com.influxdb.examples;

public class CloudReadWriteExample {
  public static void main(String[] args) {

    // Run the Flight gRPC query.
    FlightSQLReadInfluxDBClientWrite readWriteExample = new FlightSQLReadInfluxDBClientWrite();
    readWriteExample.execute();
  }
}
