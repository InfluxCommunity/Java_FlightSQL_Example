package com.influxdb.examples;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.events.WriteSuccessEvent;
import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import org.apache.arrow.flight.*;
import org.apache.arrow.flight.auth2.BearerCredentialWriter;
import org.apache.arrow.flight.grpc.CredentialCallOption;
import org.apache.arrow.flight.sql.FlightSqlClient;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.ArrowType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;

public class FlightSQLReadInfluxDBClientWrite {

    /* Get server credentials from environment variables */
    //ReadConfigs
    private static final String SOURCE_HOST = System.getenv("SOURCE_HOST");
    private static final String READ_TOKEN = System.getenv("READ_TOKEN");
    private static final String SOURCE_DATABASE_NAME = System.getenv("SOURCE_DATABASE_NAME");
    private static final String DOWNSAMPLE_QUERY = System.getenv("DOWNSAMPLE_QUERY");

    //WriteConfigs
    private static final String TARGET_CLUSTER_URL = System.getenv("TARGET_CLUSTER_URL");
    private static final String WRITE_TOKEN = System.getenv("WRITE_TOKEN");
    private static final String TARGET_DATABASE_NAME = System.getenv("TARGET_DATABASE_NAME");
    private static final String TARGET_TABLE_NAME = System.getenv("TARGET_TABLE_NAME");

    public void execute() {

        System.out.println("== Source Configs ==");
        System.out.println("SOURCE_HOST :" + SOURCE_HOST);
        System.out.println("READ_TOKEN :" + READ_TOKEN);
        System.out.println("SOURCE_DATABASE_NAME :" + SOURCE_DATABASE_NAME);
        System.out.println("DOWNSAMPLE_QUERY :" + DOWNSAMPLE_QUERY);
        System.out.println("== Target Configs ==");
        System.out.println("TARGET_CLUSTER_URL :" + TARGET_CLUSTER_URL);
        System.out.println("WRITE_TOKEN :" + WRITE_TOKEN);
        System.out.println("TARGET_DATABASE_NAME :" + TARGET_DATABASE_NAME);
        System.out.println("TARGET_TABLE_NAME :" + TARGET_TABLE_NAME);
        // Initialize Read
        System.out.println("Initializing FlightSQL client");
        FlightSqlClient sqlClient = getFlightSqlClient();
        System.out.println("sqlClient: " + sqlClient);
        // Read
        System.out.println("Fetch DownSampled query results");
        final FlightStream stream = getDownSampleQueryFlightStream(sqlClient);
        // Write
        System.out.println("Write DownSampled query results to target");
        writeDownSampledResults(stream);

        try {
            // Close the stream and release resources.
            stream.close();
        } catch (Exception e) {
            // Handle exceptions.
            System.out.println("Error closing stream: " + e.getMessage());
        }
        try {
            // Close the client
            sqlClient.close();
        } catch (Exception e) {
            // Handle exceptions.
            System.out.println("Error closing client: " + e.getMessage());
        }
        System.out.println("Finished writing downsampled data into target database");
    }

    private FlightSqlClient getFlightSqlClient() {
        // Create an interceptor that injects header metadata (database name) in every request.
        FlightClientMiddleware.Factory f = info -> new FlightClientMiddleware() {
            @Override
            public void onBeforeSendingHeaders(CallHeaders outgoingHeaders) {
                outgoingHeaders.insert("database", SOURCE_DATABASE_NAME);
            }

            @Override
            public void onHeadersReceived(CallHeaders incomingHeaders) {

            }

            @Override
            public void onCallCompleted(CallStatus status) {

            }
        };

        // Create a gRPC+TLS channel URI with HOST and port 443.
        Location location = Location.forGrpcTls(SOURCE_HOST, 443);

        // Set the allowed memory.
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        // Create a client with the allocator and gRPC channel.
        FlightClient client = FlightClient.builder(allocator, location)
                .intercept(f)
                .build();
        System.out.println("client" + client);

        FlightSqlClient sqlClient = new FlightSqlClient(client);
        return sqlClient;
    }

    private FlightStream getDownSampleQueryFlightStream(FlightSqlClient sqlClient) {
        // Define the SQL query to execute.
        /*  Construct a bearer credential using TOKEN.
            Construct a credentials option using the bearer credential.
        */
        CredentialCallOption auth = new CredentialCallOption(new BearerCredentialWriter(READ_TOKEN));

        /*  Execute the query.
            If successful, execute returns a FlightInfo object that contains metadata
            and an endpoints list.
            Each endpoint contains the following:
                - A list of addresses where you can retrieve the data.
                - A `ticket` value that identifies the data to retrieve.
        */
        FlightInfo flightInfo = sqlClient.execute(DOWNSAMPLE_QUERY, auth);

        // Extract the Flight ticket from the response.
        Ticket ticket = flightInfo.getEndpoints().get(0).getTicket();

        // Pass the ticket to request the Arrow stream data from the endpoint.
        final FlightStream stream = sqlClient.getStream(ticket, auth);
        return stream;
    }

    private void writeDownSampledResults(FlightStream stream) {
        // Process all the Arrow stream data.
        int rowCount = 0;
        while (stream.next()) {
            try {
                // Get the current vector data from the stream.
                final VectorSchemaRoot root = stream.getRoot();
                rowCount += root.getRowCount();

                InfluxDBClient influxDBClient = InfluxDBClientFactory.create(TARGET_CLUSTER_URL, WRITE_TOKEN.toCharArray(), "", TARGET_DATABASE_NAME);
                CountDownLatch countDownLatch = new CountDownLatch(1);
                try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder()
                        .batchSize(5000)
                        .flushInterval(1000)
                        .backpressureStrategy(BackpressureOverflowStrategy.DROP_OLDEST)
                        .bufferLimit(10000)
                        .jitterInterval(1000)
                        .retryInterval(5000)
                        .build())) {
                    writeApi.listenEvents(WriteSuccessEvent.class, (value) -> countDownLatch.countDown());
                    // Prepare measurement for each row & write it into target
                    writeAsPoints(writeApi, root);
                    writeApi.flush();
                }
            } catch (Exception e) {
                // Handle exceptions.
                System.out.println("Error executing FlightSqlClient: " + e.getMessage());
            }
        }
        System.out.println("No. of rows written to target: " + rowCount);
    }

    private void writeAsPoints(WriteApi writeApi, VectorSchemaRoot root) {
        long rowCount = root.getRowCount();
        int fields = root.getSchema().getFields().size();
        for(int i = 0; i < rowCount; i++) {
            Point point = Point.measurement(TARGET_TABLE_NAME);
            for (int j=0;j<fields;j++) {
                String fieldName = root.getSchema().getFields().get(j).getName();
                ArrowType fieldType = root.getSchema().getFields().get(j).getType();
                if (fieldName.equalsIgnoreCase("time") && fieldType instanceof ArrowType.Timestamp) {
                    point.time(((LocalDateTime) root.getFieldVectors().get(j)
                        .getObject(i)).atZone(ZoneId.systemDefault()).toInstant(), WritePrecision.NS);
                } else {
                    point.addField(fieldName, root.getFieldVectors().get(j).getObject(i).toString());
                }
            }
            writeApi.writePoint(TARGET_DATABASE_NAME, TARGET_TABLE_NAME, point);
        }
    }
}
