package ru.axelmark.pressuretracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;

public class NotificationSettingsActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "NotificationPrefs";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_MINUTE = "minute";
    public static final String KEY_FREQUENCY = "frequency";
    public static final String KEY_WEEK_DAY = "week_day";
    private static final int REQUEST_CODE_EXACT_ALARM = 1004;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1005;

    private SwitchMaterial switchEnabled;
    private TimePicker timePicker;
    private MaterialButtonToggleGroup frequencyGroup;
    private MaterialButtonToggleGroup weekDayGroup;
    private LinearLayout weeklyOptionsContainer;
    private Button saveButton;
    private Button testButton;
    private TextView notificationWarning;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройка уведомлений");
        }

        checkNotificationPermission();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        notificationWarning = findViewById(R.id.notificationWarning);

        switchEnabled = findViewById(R.id.switchEnabled);
        timePicker = findViewById(R.id.timePicker);
        frequencyGroup = findViewById(R.id.frequencyGroup);
        weekDayGroup = findViewById(R.id.weekDayGroup);
        weeklyOptionsContainer = findViewById(R.id.weeklyOptionsContainer);
        saveButton = findViewById(R.id.saveButton);
        testButton = findViewById(R.id.testButton);

        loadSettings();
        timePicker.setIs24HourView(true);

        // Проверка глобального состояния уведомлений
        if (!NotificationUtils.areNotificationsGloballyEnabled(this)) {
            disableAllControls();
            notificationWarning.setVisibility(View.VISIBLE);
        } else {
            notificationWarning.setVisibility(View.GONE);
        }

        frequencyGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.radioWeekly) {
                    weeklyOptionsContainer.setVisibility(View.VISIBLE);
                    if (switchEnabled.isChecked()) {
                        weekDayGroup.setEnabled(true);
                    }
                } else {
                    weeklyOptionsContainer.setVisibility(View.GONE);
                }
            }
        });

        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!NotificationUtils.areNotificationsGloballyEnabled(this)) {
                buttonView.setChecked(false);
                Toast.makeText(this,
                        "Включите уведомления в главных настройках",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            timePicker.setEnabled(isChecked);
            frequencyGroup.setEnabled(isChecked);
            saveButton.setEnabled(isChecked);

            if (isChecked && frequencyGroup.getCheckedButtonId() == R.id.radioWeekly) {
                weekDayGroup.setEnabled(true);
            } else {
                weekDayGroup.setEnabled(false);
            }
        });

        saveButton.setOnClickListener(v -> saveSettings());
        testButton.setOnClickListener(v -> testNotification());
    }

    private void disableAllControls() {
        switchEnabled.setChecked(false);
        switchEnabled.setEnabled(false);
        timePicker.setEnabled(false);
        frequencyGroup.setEnabled(false);
        saveButton.setEnabled(false);
        weekDayGroup.setEnabled(false);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Разрешение требуется")
                        .setMessage("Для работы уведомлений необходимо разрешение. Вы можете включить его в настройках.")
                        .setPositiveButton("Настройки", (d, w) -> openAppSettings())
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void loadSettings() {
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false) &&
                NotificationUtils.areNotificationsGloballyEnabled(this);
        int hour = prefs.getInt(KEY_HOUR, 9);
        int minute = prefs.getInt(KEY_MINUTE, 0);
        int frequencyId = prefs.getInt(KEY_FREQUENCY, R.id.radioDaily);
        int weekDay = prefs.getInt(KEY_WEEK_DAY, Calendar.MONDAY);

        switchEnabled.setChecked(enabled);
        timePicker.setEnabled(enabled);
        frequencyGroup.setEnabled(enabled);
        saveButton.setEnabled(enabled);

        timePicker.setHour(hour);
        timePicker.setMinute(minute);
        frequencyGroup.check(frequencyId);

        int weekDayButtonId = getButtonIdForWeekDay(weekDay);
        weekDayGroup.check(weekDayButtonId);

        if (frequencyId == R.id.radioWeekly) {
            weeklyOptionsContainer.setVisibility(View.VISIBLE);
            weekDayGroup.setEnabled(enabled);
        } else {
            weeklyOptionsContainer.setVisibility(View.GONE);
        }
    }

    private int getButtonIdForWeekDay(int weekDay) {
        if (weekDay == Calendar.MONDAY) {
            return R.id.btnMonday;
        } else if (weekDay == Calendar.TUESDAY) {
            return R.id.btnTuesday;
        } else if (weekDay == Calendar.WEDNESDAY) {
            return R.id.btnWednesday;
        } else if (weekDay == Calendar.THURSDAY) {
            return R.id.btnThursday;
        } else if (weekDay == Calendar.FRIDAY) {
            return R.id.btnFriday;
        } else if (weekDay == Calendar.SATURDAY) {
            return R.id.btnSaturday;
        } else if (weekDay == Calendar.SUNDAY) {
            return R.id.btnSunday;
        }
        return R.id.btnMonday;
    }

    private int getWeekDayForButtonId(int buttonId) {
        if (buttonId == R.id.btnMonday) {
            return Calendar.MONDAY;
        } else if (buttonId == R.id.btnTuesday) {
            return Calendar.TUESDAY;
        } else if (buttonId == R.id.btnWednesday) {
            return Calendar.WEDNESDAY;
        } else if (buttonId == R.id.btnThursday) {
            return Calendar.THURSDAY;
        } else if (buttonId == R.id.btnFriday) {
            return Calendar.FRIDAY;
        } else if (buttonId == R.id.btnSaturday) {
            return Calendar.SATURDAY;
        } else if (buttonId == R.id.btnSunday) {
            return Calendar.SUNDAY;
        }
        return Calendar.MONDAY;
    }

    private void saveSettings() {
        if (!NotificationUtils.areNotificationsGloballyEnabled(this)) {
            Toast.makeText(this, "Уведомления отключены в главных настройках", Toast.LENGTH_LONG).show();
            return;
        }

        boolean enabled = switchEnabled.isChecked();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        int frequencyId = frequencyGroup.getCheckedButtonId();
        int weekDay = Calendar.MONDAY;

        if (frequencyId == R.id.radioWeekly) {
            int selectedButtonId = weekDayGroup.getCheckedButtonId();
            if (selectedButtonId != View.NO_ID) {
                weekDay = getWeekDayForButtonId(selectedButtonId);
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ENABLED, enabled);
        editor.putInt(KEY_HOUR, hour);
        editor.putInt(KEY_MINUTE, minute);
        editor.putInt(KEY_FREQUENCY, frequencyId);
        editor.putInt(KEY_WEEK_DAY, weekDay);
        editor.apply();

        setupReminders(enabled, hour, minute, frequencyId, weekDay);
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setupReminders(boolean enabled, int hour, int minute, int frequencyId, int weekDay) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        if (enabled && NotificationUtils.areNotificationsGloballyEnabled(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    requestExactAlarmPermission();
                    return;
                }
            }

            Calendar calendar = calculateNextAlarm(hour, minute, frequencyId, weekDay);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

            Log.d("NotificationSetup", "Уведомление установлено: " + calendar.getTime());
        }
    }

    private Calendar calculateNextAlarm(int hour, int minute, int frequencyId, int weekDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (frequencyId == R.id.radioWeekly) {
            int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
            int daysUntilTarget = (weekDay - currentDay + 7) % 7;
            calendar.add(Calendar.DAY_OF_YEAR, daysUntilTarget);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 7);
            }
        } else {
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return calendar;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestExactAlarmPermission() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + getPackageName()));

        try {
            startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Функция недоступна на вашем устройстве", Toast.LENGTH_SHORT).show();
        }
    }

    private void testNotification() {
        if (!NotificationUtils.areNotificationsGloballyEnabled(this)) {
            Toast.makeText(this, "Уведомления отключены в главных настройках", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Сначала предоставьте разрешение на уведомления", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "pressure_test_channel";
        createTestChannel(manager, channelId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Тестовое уведомление")
                .setContentText("Это проверка работы напоминаний в приложении Трекер давления")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(soundUri);
        builder.setVibrate(new long[]{0, 500, 250, 500});

        if (manager != null) {
            manager.notify(999, builder.build());
            Toast.makeText(this, "Тестовое уведомление отправлено", Toast.LENGTH_SHORT).show();
        }
    }

    private void createTestChannel(NotificationManager manager, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Test Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            channel.setSound(soundUri, audioAttributes);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EXACT_ALARM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                    saveSettings();
                } else {
                    Toast.makeText(this, "Разрешение не предоставлено", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}