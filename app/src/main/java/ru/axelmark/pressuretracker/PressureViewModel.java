package ru.axelmark.pressuretracker;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class PressureViewModel extends AndroidViewModel {
    private PressureRepository repository;
    private LiveData<List<PressureMeasurement>> allMeasurements;

    public PressureViewModel(Application application) {
        super(application);
        repository = new PressureRepository(application);
        allMeasurements = repository.getAllMeasurements();
    }

    public LiveData<List<PressureMeasurement>> getAllMeasurements() {
        return allMeasurements;
    }

    public void insert(PressureMeasurement measurement) {
        repository.insert(measurement);
    }

    public void delete(PressureMeasurement measurement) {
        repository.delete(measurement);
    }
}