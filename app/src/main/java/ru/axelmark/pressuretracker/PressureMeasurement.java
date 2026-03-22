package ru.axelmark.pressuretracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import java.util.Date;

@Entity(tableName = "measurements")
public class PressureMeasurement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int systolic;
    public int diastolic;
    public int pulse;
    public long timestamp;
    public String note;
    public boolean medicationTaken;

    @Ignore
    public PressureMeasurement(int systolic, int diastolic, int pulse, String note) {
        this(systolic, diastolic, pulse, note, false);
    }

    public PressureMeasurement(int systolic, int diastolic, int pulse, String note, boolean medicationTaken) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.pulse = pulse;
        this.note = note;
        this.medicationTaken = medicationTaken;
        this.timestamp = new Date().getTime();
    }

    public String getPressureCategory() {
        if (systolic < 90 || diastolic < 60) return "low";
        if (systolic <= 120 && diastolic <= 80) return "normal";
        if (systolic <= 139 && diastolic <= 89) return "elevated";
        return "high";
    }
}