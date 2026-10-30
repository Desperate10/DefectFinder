package org.poe.defectfinder;

import android.app.Application;

import androidx.room.Room;

public class App extends Application {

    public static App instance;

    private ObjectsDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = Room.databaseBuilder(this, ObjectsDatabase.class, "database")
                .addMigrations(ObjectsDatabase.MIGRATION_1_2, ObjectsDatabase.MIGRATION_2_3, ObjectsDatabase.MIGRATION_3_4, ObjectsDatabase.MIGRATION_4_5, ObjectsDatabase.MIGRATION_5_6)
                .allowMainThreadQueries()
                .build();
    }

    public static App getInstance() {
        return instance;
    }

    public ObjectsDatabase getDatabase() {
        return database;
    }
}
