# CloudDedicated
# Java_FlightSqlClient

The Java example is a standalone Java application for querying
Apache Arrow Flight database servers like InfluxDB Cloud Dedicated using RPC and Flight SQL and writing the results back into InfluxDB Cloud Dedicated.

## Description

The example shows how to create a Java application that uses
Apache Arrow Flight (`org.apache.arrow.flight`)
and Flight SQL (`org.apache.arrow.flight.sql`) packages to 
interact with a Flight database server,perform downsampling and write back to InfluxDB Cloud Dedicated.

You can use the example to connect to InfluxDB Cloud Dedicated, execute database commands and
SQL queries,retrieve data and write to a different database.

## Build and run the Java application

1. [Prerequisites](#Prerequisites)
2. [Build](#build)
3. [Run](#run)

### Prerequisites

Running the application requires the following:

- **Docker**: The example project uses Docker to ensure a consistent build environment. Follow the instructions to download and install Docker for your system:

    - **macOS**: [Install Docker for macOS](https://docs.docker.com/desktop/install/mac-install/)
    - **Linux**: [Install Docker for Linux](https://docs.docker.com/desktop/install/linux-install/)
              
- **Source Database**: The name of a Flight database to query--for example, an [InfluxDB Cloud Dedicated Source Bucket](https://docs.influxdata.com/influxdb/cloud-dedicated/get-started/setup/#create-a-database).
- **Source Cluster URL**: The hostname of the Flight server--for example, your [InfluxDB Cloud Dedicated Source cluster URL](https://docs.influxdata.com/influxdb/cloud-dedicated/get-started/setup/#request-an-influxdb-cloud-dedicated-cluster) without the protocol("https://").
- **Source database Token**: An authentication `Bearer` token with _read_ permission to the database--for example, an [InfluxDB Cloud Dedicated Source database Token][(https://docs.influxdata.com/influxdb/cloud-dedicated/get-started/setup/#create-a-database-token)].
- **Target Database**: The name of a Flight database to query--for example, an [InfluxDB Cloud Dedicated Target Bucket](https://docs.influxdata.com/influxdb/cloud-dedicated/get-started/setup/#create-a-database).
- **Target Cluster URL**: The hostname of the Flight server--for example, your [InfluxDB Cloud Dedicated Target cluster URL](https://docs.influxdata.com/influxdb/cloud-dedicated/get-started/setup/#request-an-influxdb-cloud-dedicated-cluster) include the protocol("https://").
- **Target database Name**: The Target database name to write the query output [InfluxDB Cloud Dedicated target table name]
- **Query**: Downsample query.You can replace with any query based on your need. 

### Build

The project contains an `influxdb-build.sh` script that you can use with InfluxDB Cloud Dedicated or as an example to create your build script.

#### Example: Build for InfluxDB Cloud

1. In your terminal, set the following environment variables.

    ```sh
    # Set environment variables

     export INFLUX_SRC_CLUSTER_URL=t1234567-e00r-56s4-9f21-d36eu913fb7.a.influxdb.io && \
     export INFLUX_TGT_CLUSTER_URL=https://t1234567-e00r-56s4-9f21-d36eu913fb7.a.influxdb.io && \
     export INFLUX_SRC_DB_NAME=sourcedata && \
     export INFLUX_TGT_DB_NAME=downsampled && \
     export INFLUX_READ_TOKEN=Nuy5fQ6J5bCLGh3igU4mCL/F0qWIOaGpJ && \
     export INFLUX_WRITE_TOKEN=Rw8KFP7SMz1ynf5bc/qEXGNCsTDkF0eU/9 && \
     export INFLUX_TGT_TBL_NAME=airSensors && \
     export QUERY="SELECT DATE_BIN(INTERVAL '1 minute', time, '2019-09-18T00:00:00Z'::timestamp) as time, SUM(\"co\") as 'sum_co', SUM(\"temperature\") as 'sum_temp', SUM(\"humidity\") as 'sum_hum' FROM \"airSensors\" WHERE time >= now() - interval '9 day' GROUP BY time"
    ```

2. Run the following:
    - `sh ./influxdb-build.sh build`

The script builds an image with the name `javaflight`.

### Run

To start the application, run `docker run <IMAGE_NAME>` in your terminal.

```sh
docker run javaflight
```
