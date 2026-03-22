package ru.axelmark.pressuretracker;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {
    private LineChart chart;
    private PressureViewModel viewModel;
    private List<PressureMeasurement> allMeasurements = new ArrayList<>();
    private MaterialButtonToggleGroup periodToggleGroup;
    private TextView periodTitle;
    private TextView emptyChartText;
    private boolean isFirstLoad = true;

    // Форматтеры даты
    private final SimpleDateFormat dailyFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    private final SimpleDateFormat hourlyFormat = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        chart = findViewById(R.id.chart);
        periodToggleGroup = findViewById(R.id.periodToggleGroup);
        periodTitle = findViewById(R.id.periodTitle);
        emptyChartText = findViewById(R.id.emptyChartText);

        // Настройка графика
        configureChartAppearance();

        // Настраиваем кнопки периода через ToggleGroup
        periodToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !allMeasurements.isEmpty()) {
                if (checkedId == R.id.btnWeek) {
                    showDataForPeriod(7);
                } else if (checkedId == R.id.btnMonth) {
                    showDataForPeriod(30);
                } else if (checkedId == R.id.btnYear) {
                    showDataForPeriod(365);
                }
            }
        });

        viewModel = new ViewModelProvider(this).get(PressureViewModel.class);
        viewModel.getAllMeasurements().observe(this, measurements -> {
            if (measurements != null && !measurements.isEmpty()) {
                // Сортируем измерения по дате
                Collections.sort(measurements, (m1, m2) ->
                        Long.compare(m1.timestamp, m2.timestamp));
                allMeasurements = measurements;

                if (isFirstLoad) {
                    // При первом запуске автоматически выбираем неделю
                    periodToggleGroup.check(R.id.btnWeek);
                    isFirstLoad = false;
                } else {
                    // При обновлении данных перерисовываем текущий период
                    int checkedId = periodToggleGroup.getCheckedButtonId();
                    if (checkedId != View.NO_ID) {
                        periodToggleGroup.check(checkedId); // Триггер обновления
                    }
                }
            } else {
                showEmptyState();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDataForPeriod(int days) {
        if (allMeasurements.isEmpty()) {
            showEmptyState();
            return;
        }

        // Фильтруем данные по периоду
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (days * 24 * 60 * 60 * 1000L);

        List<PressureMeasurement> periodMeasurements = new ArrayList<>();
        for (PressureMeasurement m : allMeasurements) {
            if (m.timestamp >= startTime) {
                periodMeasurements.add(m);
            }
        }

        if (periodMeasurements.isEmpty()) {
            showEmptyState();
            return;
        }

        // Устанавливаем заголовок периода
        String periodText;
        if (days == 7) {
            periodText = getString(R.string.week_period);
        } else if (days == 30) {
            periodText = getString(R.string.month_period);
        } else {
            periodText = getString(R.string.year_period);
        }
        periodTitle.setText(periodText);

        // Создаем график для выбранного периода
        createChart(periodMeasurements);
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        emptyChartText.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        chart.setVisibility(View.VISIBLE);
        emptyChartText.setVisibility(View.GONE);
    }

    private void configureChartAppearance() {
        // Настраиваем горизонтальную прокрутку
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setHorizontalScrollBarEnabled(true);

        // Отключаем ненужные элементы
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);

        // Настройка осей
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void createChart(List<PressureMeasurement> measurements) {
        hideEmptyState();

        List<Entry> systolicEntries = new ArrayList<>();
        List<Entry> diastolicEntries = new ArrayList<>();
        List<Entry> pulseEntries = new ArrayList<>();

        for (int i = 0; i < measurements.size(); i++) {
            PressureMeasurement m = measurements.get(i);
            systolicEntries.add(new Entry(i, m.systolic));
            diastolicEntries.add(new Entry(i, m.diastolic));
            pulseEntries.add(new Entry(i, m.pulse));
        }

        // Создаем наборы данных
        LineDataSet systolicDataSet = createDataSet(
                systolicEntries,
                getString(R.string.systolic),
                ContextCompat.getColor(this, R.color.systolic_color)
        );

        LineDataSet diastolicDataSet = createDataSet(
                diastolicEntries,
                getString(R.string.diastolic),
                ContextCompat.getColor(this, R.color.diastolic_color)
        );

        LineDataSet pulseDataSet = createDataSet(
                pulseEntries,
                getString(R.string.pulse),
                ContextCompat.getColor(this, R.color.pulse_color)
        );
        pulseDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

        // Настраиваем оси
        configureAxes(measurements);

        // Собираем все данные вместе
        LineData lineData = new LineData(systolicDataSet, diastolicDataSet, pulseDataSet);
        chart.setData(lineData);

        // Настраиваем легенду
        chart.getLegend().setTextColor(
                ContextCompat.getColor(this, R.color.md3_on_surface)
        );

        chart.invalidate(); // Обновляем график
        chart.animateY(1000); // Анимация

        // Прокручиваем к последнему элементу
        chart.moveViewToX(measurements.size() - 1);
    }

    private LineDataSet createDataSet(List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(
                ContextCompat.getColor(this, R.color.md3_on_surface)
        );
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        return dataSet;
    }

    private void configureAxes(List<PressureMeasurement> measurements) {
        // Настраиваем ось Y для давления
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setTextColor(
                ContextCompat.getColor(this,R.color.md3_on_surface)
        );
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.valueOf((int) value) + " mmHg";
            }
        });

        // Настраиваем ось Y для пульса
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(true);
        rightAxis.setAxisMinimum(40f);
        rightAxis.setAxisMaximum(120f);
        rightAxis.setTextColor(
                ContextCompat.getColor(this, R.color.pulse_color)
        );
        rightAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.valueOf((int) value) + " bpm";
            }
        });

        // Настраиваем ось X
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < measurements.size()) {
                    Date date = new Date(measurements.get(index).timestamp);

                    // Выбираем форматтер в зависимости от количества точек
                    SimpleDateFormat sdf = measurements.size() > 14 ?
                            dailyFormat : hourlyFormat;

                    return sdf.format(date);
                }
                return "";
            }
        });
    }
}