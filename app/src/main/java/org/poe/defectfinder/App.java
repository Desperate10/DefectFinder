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
