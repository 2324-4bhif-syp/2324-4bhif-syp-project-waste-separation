package at.htl.influxdb;

import at.htl.entity.Measurement;
import at.htl.entity.Measurement_Table;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class JsonToInfluxDB {

    public static void queryAllData() {
        String token = "-PHNf93K8rwPKfgmBjmwSCfvqyQ3Nf6eE0R9pIml4K0VtJCoSTNtuCMKi4BVsds5nCXvhsLoEFUOQNXJsF3bPA==";
        String bucket = "db";
        String org = "Leoenergy";
        String influxUrl = "http://localhost:8086";

        try {
            InfluxDBClient client = InfluxDBClientFactory.create(influxUrl, token.toCharArray());

            String query = "from(bucket: \"db\") |> range(start: -1h) |> filter(fn: (r) => r._measurement == \"Measurement_Table\")";
            List<FluxTable> tables = client.getQueryApi().query(query, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    System.out.println(record);
                }
            }

            client.close();

        } catch (Exception e) {
            System.err.println("Error querying data from InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void writeToInfluxDB(Measurement_Table measurementTable) {
        String token = "-PHNf93K8rwPKfgmBjmwSCfvqyQ3Nf6eE0R9pIml4K0VtJCoSTNtuCMKi4BVsds5nCXvhsLoEFUOQNXJsF3bPA==";
        String bucket = "db";
        String org = "Leoenergy";
        String influxUrl = "http://localhost:8086";
        try {
            InfluxDBClient client = InfluxDBClientFactory.create(influxUrl, token.toCharArray());
            WriteApiBlocking writeApi = client.getWriteApiBlocking();

            System.out.println("InstanceNOW:  " + Instant.now());
            System.out.println("MyInstance: " + measurementTable.getInstant());

            Point point = Point.measurement("Measurement_Table")
                    .addTag("id", measurementTable.getId().toString())
                    .addField("value", measurementTable.getValue())
                    .time(measurementTable.getInstant(), WritePrecision.NS);

            writeApi.writePoint(bucket, org, point);
            client.close();

        } catch (Exception e) {
            System.err.println("Error writing    data to InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }
    }




    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");


        Measurement_Table measurementTable = new Measurement_Table(BigInteger.valueOf(3l),
                Instant.ofEpochMilli(1670266030),
                BigDecimal.valueOf(2000.321),
                new Measurement());
        writeToInfluxDB(measurementTable);
        queryAllData();



    }
}
