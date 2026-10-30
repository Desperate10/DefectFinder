package org.poe.defectfinder;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.entities.Objects;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Objects.class, Defects.class}, version = 6, exportSchema = false)
public abstract class ObjectsDatabase extends RoomDatabase {

    abstract ObjectsDao objectsDao();
    abstract DefectsDao defectsDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Legacy rows default to 0 (low voltage). We cannot reliably infer the
            // original class from the stored type int: R.raw.* ids are not stable
            // across builds, so any heuristic match produces false positives.
            database.execSQL("ALTER TABLE objects ADD COLUMN highVoltage INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE defects ADD COLUMN thermalDoneOk INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE defects ADD COLUMN thermalNotDone INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE defects ADD COLUMN defectsFound INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE defects ADD COLUMN defectsAbsent INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE objects ADD COLUMN thermalDoneOk INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE objects ADD COLUMN thermalNotDone INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE objects ADD COLUMN defectsFound INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE objects ADD COLUMN defectsAbsent INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Lists uploaded before this version get 0 and stay locked: their send time is unknown.
            database.execSQL("ALTER TABLE objects ADD COLUMN sentAt INTEGER NOT NULL DEFAULT 0");
        }
    };
}
