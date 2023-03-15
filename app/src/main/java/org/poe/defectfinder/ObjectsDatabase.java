package org.poe.defectfinder;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.entities.Objects;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Objects.class, Defects.class}, version = 1, exportSchema = false)
public abstract class ObjectsDatabase extends RoomDatabase {

    abstract ObjectsDao objectsDao();
    abstract DefectsDao defectsDao();

}
