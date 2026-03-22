package ru.axelmark.pressuretracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationUtils {
    private static final String GLOBAL_PREFS_NAME = "GlobalNotificationPrefs";
    private static final String KEY_GLOBAL_ENABLED = "global_notifications_enabled";

    public static void setGlobalNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_GLOBAL_ENABLED, enabled).apply();

        if (!enabled) {
            cancelAllReminders(context);
        }
    }

    public static boolean areNotificationsGloballyEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_GLOBAL_ENABLED, true);
    }

    private static void cancelAllReminders(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}