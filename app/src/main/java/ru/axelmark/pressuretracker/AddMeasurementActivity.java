package ru.axelmark.pressuretracker;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMeasurementActivity extends AppCompatActivity {
    private static final int MIN_SYSTOLIC = 40;
    private static final int MAX_SYSTOLIC = 300;
    private static final int MIN_DIASTOLIC = 20;
    private static final int MAX_DIASTOLIC = 200;
    private static final int MIN_PULSE = 20;
    private static final int MAX_PULSE = 250;

    private TextInputLayout systolicLayout;
    private TextInputLayout diastolicLayout;
    private TextInputLayout pulseLayout;
    private TextInputLayout noteLayout;
    private TextView currentDate;
    private SwitchMaterial medicationSwitch;
    private PressureRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_measurement);

        initToolbar();
        initViews();
        setCurrentDate();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        repository = new PressureRepository(getApplication());

        systolicLayout = findViewById(R.id.systolicLayout);
        diastolicLayout = findViewById(R.id.diastolicLayout);
        pulseLayout = findViewById(R.id.pulseLayout);
        noteLayout = findViewById(R.id.noteLayout);
        currentDate = findViewById(R.id.currentDate);
        medicationSwitch = findViewById(R.id.medicationSwitch);

        findViewById(R.id.saveButton).setOnClickListener(v -> saveMeasurement());
    }

    private void setCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());
        currentDate.setText(sdf.format(new Date()));
    }

    private boolean validateInput() {
        boolean isValid = true;

        String systolicStr = systolicLayout.getEditText().getText().toString();
        if (systolicStr.isEmpty()) {
            systolicLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            int systolic = Integer.parseInt(systolicStr);
            if (systolic < MIN_SYSTOLIC || systolic > MAX_SYSTOLIC) {
                systolicLayout.setError(getString(R.string.error_systolic_range, MIN_SYSTOLIC, MAX_SYSTOLIC));
                isValid = false;
            } else {
                systolicLayout.setError(null);
            }
        }

        String diastolicStr = diastolicLayout.getEditText().getText().toString();
        if (diastolicStr.isEmpty()) {
            diastolicLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            int diastolic = Integer.parseInt(diastolicStr);
            if (diastolic < MIN_DIASTOLIC || diastolic > MAX_DIASTOLIC) {
                diastolicLayout.setError(getString(R.string.error_diastolic_range, MIN_DIASTOLIC, MAX_DIASTOLIC));
                isValid = false;
            } else {
                diastolicLayout.setError(null);
            }
        }

        String pulseStr = pulseLayout.getEditText().getText().toString();
        if (pulseStr.isEmpty()) {
            pulseLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            int pulse = Integer.parseInt(pulseStr);
            if (pulse < MIN_PULSE || pulse > MAX_PULSE) {
                pulseLayout.setError(getString(R.string.error_pulse_range, MIN_PULSE, MAX_PULSE));
                isValid = false;
            } else {
                pulseLayout.setError(null);
            }
        }

        return isValid;
    }

    private void saveMeasurement() {
        if (!validateInput()) {
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.check_input_data, Snackbar.LENGTH_SHORT).show();
            return;
        }

        try {
            int systolic = Integer.parseInt(systolicLayout.getEditText().getText().toString());
            int diastolic = Integer.parseInt(diastolicLayout.getEditText().getText().toString());
            int pulse = Integer.parseInt(pulseLayout.getEditText().getText().toString());
            String note = noteLayout.getEditText().getText().toString();
            boolean medicationTaken = medicationSwitch.isChecked();

            PressureMeasurement measurement = new PressureMeasurement(
                    systolic, diastolic, pulse, note, medicationTaken
            );

            repository.insert(measurement);
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.measurement_saved, Snackbar.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.error_saving_data, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}