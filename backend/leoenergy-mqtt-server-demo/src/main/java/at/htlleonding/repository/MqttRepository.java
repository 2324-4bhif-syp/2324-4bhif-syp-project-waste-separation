package at.htlleonding.repository;
import at.htlleonding.entity.Device;
import at.htlleonding.entity.SensorValue;
import at.htlleonding.pojo.SensorValuePojo;

import at.htlleonding.processor.UnitConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ApplicationScoped
public class MqttRepository {
    SensorValuePojo sensorValuePojo;

    @ConfigProperty(name = "json.file-directory-all")
    private String directoryNameAll;
    @Inject
    @Channel("leonergy-demo")
    Emitter<String> emitter;
    private AtomicInteger counter = new AtomicInteger();
    @Scheduled(every = "10s")
    public void invokeSendPeriodically(){
        importJsonFiles(directoryNameAll,1);
    }

    public void send(Map<String,String> payload,String measurementId){
        Map.Entry<String,String> entry = payload.entrySet().iterator().next();
        String topic = "leoenergy-demo/" + entry.getKey() + "/" + measurementId;
        MqttMessage<String> message = MqttMessage.of(topic,entry.getValue());
        emitter.send(message);
    }

    public Map<String,String> createPayload(SensorValue sensorValue) throws JsonProcessingException {
        Map<String,String> sensorValueDetails = new HashMap<>();
        SensorValuePojo sensorValuePojo = convertSensorValueToPojo(sensorValue);
        String json = new ObjectMapper().writeValueAsString(sensorValuePojo);
        sensorValueDetails.put(String.valueOf(sensorValue.getDevice().getId()),json);

        return sensorValueDetails;
    }

    public void importJsonFiles(String directoryName,int limit) {
        try (Stream<Path> filePathStream = Files.walk(Paths.get(directoryName)).onClose(() -> {
        })) {
            filePathStream
                    .filter(Files::isRegularFile)
                    .filter(f -> !f.toFile().isHidden())
                    .limit(limit)
                    .forEach(filePath -> {
                        try {
                            processJsonToSensorValue(filePath);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void processJsonToSensorValue(Path filePath) throws JsonProcessingException {

        ObjectMapper om = new ObjectMapper();
        JsonNode jsonRoot = null;
        Device device = null;

        try {
            jsonRoot = om.readTree(filePath.toFile());
            device = new Device(
                    jsonRoot.at("/Device/Id").asInt(),
                    jsonRoot.at("/Device/ManufacturerId").asText(),
                    jsonRoot.at("/Device/Medium").asText(),
                    jsonRoot.at("/Device/Name").asText(),
                    jsonRoot.at("/Device/Site").asText()
            );


        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        ArrayNode valueArray = (ArrayNode) jsonRoot.at("/Device/ValueDescs");
        for (JsonNode jsonNode : valueArray) {


            long dateNow = 1708367175;
            long timeOfValueInUnix =  jsonNode.get("Values").get(0).get("Timestamp").asLong();

            long diff = dateNow-timeOfValueInUnix; // differenz beetwen today 0:00 and the timestamp
            long newDate = diff + timeOfValueInUnix; // calculate new Date for every measurement

            SensorValue sensorValue = new SensorValue(device,
                    newDate,
                    UnitConverter.convertToKilowatt(
                            jsonNode.get("UnitStr").asText(),
                            jsonNode.get("Values").get(0).get("Val").doubleValue()),
                    jsonNode.get("Id").asLong(),
                    jsonNode.get("DescriptionStr").asText(),
                    jsonNode.get("UnitStr").asText());

            if ( jsonNode.get("UnitStr").asText().equals("W") || jsonNode.get("UnitStr").asText().equals("Wh")) {
                send(createPayload(sensorValue),String.valueOf(sensorValue.getMeasurementId()));
            }
        }
    }




    public SensorValuePojo getSensorValuePojo() {
        return sensorValuePojo;
    }

    public void setSensorValuePojo(SensorValuePojo sensorValuePojo) {
        this.sensorValuePojo = sensorValuePojo;
    }

    public SensorValuePojo convertSensorValueToPojo(SensorValue sensorValue){
        return  new SensorValuePojo(sensorValue.getDevice().getName(),
                sensorValue.getMeasurementId(),
                sensorValue.getDescription(),
                sensorValue.getUnit(),
                sensorValue.getTime(),
                sensorValue.getValue()
        );
    }

}