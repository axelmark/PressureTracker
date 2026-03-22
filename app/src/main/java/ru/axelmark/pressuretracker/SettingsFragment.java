package ru.axelmark.pressuretracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ExportUtils exportUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        exportUtils = new ExportUtils(requireActivity());
        setupPreferences();
    }

    private void setupPreferences() {
        // Управление профилем
        Preference userDataPref = findPreference("user_data");
        if (userDataPref != null) {
            userDataPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), UserDataActivity.class));
                return true;
            });
        }

        // Настройки уведомлений
        Preference notificationSettingsPref = findPreference("notification_settings");
        if (notificationSettingsPref != null) {
            notificationSettingsPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), NotificationSettingsActivity.class));
                return true;
            });
        }

        // Экспорт данных
        Preference exportPref = findPreference("export_data");
        if (exportPref != null) {
            exportPref.setOnPreferenceClickListener(preference -> {
                exportData();
                return true;
            });
        }

        // О программе
        Preference aboutPref = findPreference("about");
        if (aboutPref != null) {
            aboutPref.setOnPreferenceClickListener(preference -> {
                // Запускаем AboutActivity вместо диалога
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            });
        }
    }

    private void exportData() {
        new Thread(() -> {
            List<PressureMeasurement> measurements = PressureDatabase.getDatabase(requireContext())
                    .pressureDao().getAllMeasurementsSync();
            requireActivity().runOnUiThread(() -> {
                if (measurements == null || measurements.isEmpty()) {
                    Toast.makeText(requireContext(), "Нет данных для экспорта", Toast.LENGTH_SHORT).show();
                } else {
                    exportUtils.exportData(measurements);
                }
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("notifications_master_switch")) {
            boolean enabled = sharedPreferences.getBoolean(key, true);
            NotificationUtils.setGlobalNotificationsEnabled(requireContext(), enabled);
            Preference notificationSettingsPref = findPreference("notification_settings");
            if (notificationSettingsPref != null) {
                notificationSettingsPref.setEnabled(enabled);
            }
            String message = enabled ?
                    "Уведомления включены" :
                    "Все уведомления отключены. Напоминания не будут работать";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}