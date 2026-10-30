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

    // sentAt = 0 keeps new defects from being merged into a list reopened for editing after upload.
    @Query("SELECT * FROM objects WHERE name = :name and modificator =:modificator and highVoltage = :highVoltage and status = \"Не вигружено\" and sentAt = 0")
    Objects getObject(String name, String modificator, boolean highVoltage);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Objects object);

    @Query("UPDATE objects SET date =:date WHERE id =:id")
    void updateDate(int id, String date);

    // sentAt is set only on the first upload, so the edit window is not extended by resending.
    @Query("UPDATE objects SET status = \"Вигружено\", sentAt = CASE WHEN sentAt = 0 THEN :now ELSE sentAt END WHERE id =:id")
    void updateStatus(int id, long now);

    @Query("UPDATE objects SET status = \"Не вигружено\" WHERE id =:id")
    void reopen(int id);

    @Query("UPDATE objects SET modificator=:modificator WHERE id=:id")
    void updateModificator(int id, String modificator);

    @Query("UPDATE objects SET highVoltage=:highVoltage WHERE id=:id")
    void updateHighVoltage(int id, boolean highVoltage);

    @Query("SELECT * FROM objects WHERE id = :id")
    Objects getObjectById(int id);

    @Query("UPDATE objects SET thermalDoneOk=:thermalDone, thermalNotDone=:thermalNotDone, defectsFound=:defectsFound, defectsAbsent=:defectsAbsent WHERE id=:id")
    void updateThermalState(int id, boolean thermalDone, boolean thermalNotDone, boolean defectsFound, boolean defectsAbsent);

    @Query("DELETE FROM objects WHERE id = :id")
    void delete(int id);
}
