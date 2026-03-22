package ru.axelmark.pressuretracker;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class PressureRepository {
    private PressureDao pressureDao;
    private LiveData<List<PressureMeasurement>> allMeasurements;

    public PressureRepository(Application application) {
        PressureDatabase db = PressureDatabase.getDatabase(application);
        pressureDao = db.pressureDao();
        allMeasurements = pressureDao.getAllMeasurements();
    }

    public LiveData<List<PressureMeasurement>> getAllMeasurements() {
        return allMeasurements;
    }

    public void insert(PressureMeasurement measurement) {
        PressureDatabase.databaseWriteExecutor.execute(() -> {
            pressureDao.insert(measurement);
        });
    }

    public void delete(PressureMeasurement measurement) {
        PressureDatabase.databaseWriteExecutor.execute(() -> {
            pressureDao.delete(measurement);
        });
    }
}