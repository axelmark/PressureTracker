package ru.axelmark.pressuretracker;

import android.Manifest;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private PressureViewModel viewModel;
    private PressureAdapter adapter;

    // Views для пустого состояния
    private MaterialCardView emptyStateCard;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;
    private Toolbar toolbar;

    // Константы для разрешений
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private static final int EXACT_ALARM_PERMISSION_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация Toolbar
        toolbar = findViewById(R.id.toolbar); // Добавлено
        setSupportActionBar(toolbar); // Добавлено

        // Инициализация элементов пустого состояния
        emptyStateCard = findViewById(R.id.emptyStateCard);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateDescription = findViewById(R.id.emptyStateDescription);

        // Проверка разрешений
        checkNotificationPermission();
        checkExactAlarmPermission();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PressureAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(PressureViewModel.class);
        viewModel.getAllMeasurements().observe(this, measurements -> {
            if (measurements != null) {
                adapter.updateData(measurements);

                // Показываем/скрываем состояние пустого списка
                if (measurements.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                }
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, AddMeasurementActivity.class));
        });

        // Обработчик для кнопки "Добавить первое измерение"
        findViewById(R.id.emptyStateAction).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddMeasurementActivity.class));
        });

        setupSwipeToDelete(recyclerView);
    }

    private void showEmptyState() {
        emptyStateCard.setVisibility(View.VISIBLE);
        emptyStateTitle.setText("Нет измерений");
        emptyStateDescription.setText("Добавьте свое первое измерение артериального давления");
    }

    private void hideEmptyState() {
        emptyStateCard.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_chart) {
            startActivity(new Intent(this, ChartActivity.class));
            return true;
        } else if (id == R.id.action_export) {
            // Получаем данные напрямую из базы
            new Thread(() -> {
                List<PressureMeasurement> measurements =
                        PressureDatabase.getDatabase(this)
                                .pressureDao()
                                .getAllMeasurementsSync();

                runOnUiThread(() -> {
                    if (measurements == null || measurements.isEmpty()) {
                        Toast.makeText(this,
                                "Нет данных для экспорта",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        new ExportUtils(this).exportData(measurements);
                    }
                });
            }).start();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
//        else if (id == R.id.action_about) {
//            startActivity(new Intent(this, AboutActivity.class));
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSwipeToDelete(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                PressureMeasurement deletedItem = adapter.getItem(position);
                viewModel.delete(deletedItem);

                Snackbar.make(recyclerView, "Измерение удалено", Snackbar.LENGTH_LONG)
                        .setAction("Отменить", v -> viewModel.insert(deletedItem))
                        .show();

                // Проверяем, стал ли список пустым после удаления
                if (adapter.getItemCount() == 0) {
                    showEmptyState();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                View swipeBackground = itemView.findViewById(R.id.swipeBackground);
                View contentLayout = itemView.findViewById(R.id.contentLayout);

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    swipeBackground.setVisibility(View.VISIBLE);
                    contentLayout.setTranslationX(dX);

                    if (dX == 0) {
                        swipeBackground.setVisibility(View.INVISIBLE);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                View contentLayout = viewHolder.itemView.findViewById(R.id.contentLayout);
                View swipeBackground = viewHolder.itemView.findViewById(R.id.swipeBackground);

                contentLayout.setTranslationX(0);
                swipeBackground.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder != null) {
                        View swipeBackground = viewHolder.itemView.findViewById(R.id.swipeBackground);
                        if (swipeBackground != null) {
                            swipeBackground.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.5f;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    // Проверка разрешения на уведомления (Android 13+)
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    // Проверка разрешения на точные будильники (Android 12+)
    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivityForResult(intent, EXACT_ALARM_PERMISSION_CODE);
                } catch (ActivityNotFoundException e) {
                    // Игнорируем, если действие не поддерживается
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXACT_ALARM_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Без разрешения напоминания могут работать некорректно",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Без разрешения уведомления не будут работать",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }
}