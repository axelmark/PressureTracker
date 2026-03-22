package ru.axelmark.pressuretracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupUtils {
    private static final String TAG = "BackupUtils";
    private static final String BACKUP_FOLDER = "PressureTrackerBackups";
    private static final String DB_NAME = "pressure_database";
    private static final String BACKUP_PREFIX = "pressure_backup_";
    private static final String BACKUP_EXT = ".db";
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1001;

    private final Context context;

    public BackupUtils(Context context) {
        this.context = context;
    }

    // Создание резервной копии
    public void createBackup() {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                showToast("База данных не найдена");
                return;
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String backupFileName = BACKUP_PREFIX + timeStamp + BACKUP_EXT;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createBackupApi30(dbFile, backupFileName);
            } else {
                createBackupLegacy(dbFile, backupFileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            showToast("Ошибка создания резервной копии");
        }
    }

    // Восстановление из резервной копии
    public void restoreBackup(Uri backupUri) {
        try {
            // 1. Получаем путь к текущей БД
            File dbFile = context.getDatabasePath(DB_NAME);

            // 2. Создаем временный файл для восстановления
            File tempFile = new File(context.getCacheDir(), "temp_restore.db");

            // 3. Копируем данные из резервной копии во временный файл
            try (InputStream in = context.getContentResolver().openInputStream(backupUri);
                 OutputStream out = new FileOutputStream(tempFile)) {
                if (in == null) {
                    throw new IOException("Не удалось открыть резервную копию");
                }
                copyFile(in, out);
            }

            // 4. Закрываем подключение к текущей БД
            PressureDatabase.getDatabase(context).close();

            // 5. Удаляем текущую БД
            if (dbFile.exists() && !dbFile.delete()) {
                throw new IOException("Не удалось удалить текущую БД");
            }

            // 6. Копируем временный файл в место расположения БД
            if (!tempFile.renameTo(dbFile)) {
                throw new IOException("Не удалось восстановить БД из резервной копии");
            }

            showToast("Данные успешно восстановлены");
        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            showToast("Ошибка восстановления данных: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void createBackupApi30(File dbFile, String backupFileName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, backupFileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + BACKUP_FOLDER);

        Uri uri = context.getContentResolver().insert(
                MediaStore.Files.getContentUri("external"),
                values
        );

        if (uri == null) {
            showToast("Не удалось создать файл резервной копии");
            return;
        }

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("Не удалось открыть поток для записи");
            }
            copyFile(in, out);
            showToast("Резервная копия создана: " + backupFileName);
        }
    }

    private void createBackupLegacy(File dbFile, String backupFileName) throws IOException {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER);

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            throw new IOException("Не удалось создать директорию для резервных копий");
        }

        File backupFile = new File(backupDir, backupFileName);

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = new FileOutputStream(backupFile)) {
            copyFile(in, out);

            // Обновляем медиасканер
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(backupFile));
            context.sendBroadcast(mediaScanIntent);

            showToast("Резервная копия создана: " + backupFile.getAbsolutePath());
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        out.flush();
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}