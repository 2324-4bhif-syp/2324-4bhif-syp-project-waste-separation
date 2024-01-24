package at.htl.leoenergy.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Entity
@Table(name = "SENSOR_VALUE_DETAILS")
public class SensorDetails {
    @Id
    private Long id;

    @ManyToOne(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH
    })
    private Device device;

    @Column(name = "DESCRIPTION")
    private String descriptionStr;

    @Column(name = "UNIT")
    private String unitStr;

    public SensorDetails() {
    }

    public SensorDetails(Long id,Device device, String descriptionStr, String unitStr) {
        this.id = id;
        this.device = device;
        this.descriptionStr = descriptionStr;
        this.unitStr = unitStr;


    }

    //region getter and setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }



    public String getDescriptionStr() {
        return descriptionStr;
    }

    public void setDescriptionStr(String descriptionStr) {
        this.descriptionStr = descriptionStr;
    }

    public String getUnitStr() {
        return unitStr;
    }

    public void setUnitStr(String unitStr) {
        this.unitStr = unitStr;
    }



    //endregion

}