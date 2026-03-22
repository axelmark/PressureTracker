package ru.axelmark.pressuretracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PressureDao {
    @Insert
    void insert(PressureMeasurement measurement);

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    LiveData<List<PressureMeasurement>> getAllMeasurements();

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    List<PressureMeasurement> getAllMeasurementsSync();

    @Delete
    void delete(PressureMeasurement measurement);
}