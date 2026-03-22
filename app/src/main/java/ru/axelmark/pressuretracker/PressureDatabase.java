package ru.axelmark.pressuretracker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {PressureMeasurement.class}, version = 2)
public abstract class PressureDatabase extends RoomDatabase {
    public abstract PressureDao pressureDao();

    private static volatile PressureDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measurements ADD COLUMN medicationTaken INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static PressureDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PressureDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    PressureDatabase.class,
                                    "pressure_database"
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static List<PressureMeasurement> getMeasurementsSync(Context context) {
        return getDatabase(context).pressureDao().getAllMeasurementsSync();
    }
}