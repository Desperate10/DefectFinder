package org.poe.defectfinder.dao;

import org.poe.defectfinder.entities.Objects;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ObjectsDao {

    @Query("SELECT * FROM objects where modificator <> \"confirm\"")
    List<Objects> getAllAddEdit();

    @Query("SELECT * FROM objects where modificator LIKE \"confirm\"")
    List<Objects> getAllConfirm();

    @Query("SELECT * FROM objects WHERE name = :name and modificator =:modificator and status = \"Не вигружено\"")
    Objects getObject(String name, String modificator);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Objects object);

    @Query("UPDATE objects SET date =:date WHERE id =:id")
    void updateDate(int id, String date);

    @Query("UPDATE objects SET status = \"Вигружено\" WHERE id =:id")
    void updateStatus(int id);

    @Query("UPDATE objects SET modificator=:modificator WHERE id=:id")
    void updateModificator(int id, String modificator);

    @Query("DELETE FROM objects WHERE id = :id")
    void delete(int id);
}
