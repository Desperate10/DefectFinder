package org.poe.defectfinder.dao;

import org.poe.defectfinder.entities.Defects;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DefectsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Defects defect);

    @Query("SELECT * FROM defects WHERE parentObjectId = :id")
    List<Defects> getDefectsByObject(int id);

    @Query("SELECT * FROM defects WHERE parentObjectId = :id AND oprName = :opr")
    List<Defects> getDefectByObjectOprName(int id, String opr);

    @Query("SELECT DISTINCT oprName from defects WHERE parentObjectId =:id ORDER BY oprName")
    List<String> getOprList(int id);

    @Query("SELECT * FROM defects WHERE parentObjectId = :id AND oprName = :opr AND defectName = :defect")
    Defects getCurtainDefect(int id, String opr, String defect);

    @Query("UPDATE defects SET defectName = :defect WHERE parentObjectId = :id AND oprName = :opr AND defectName =:oldDefectName")
    void updateDefect(String defect, int id, String opr, String oldDefectName);

    @Query("UPDATE defects SET photos = :photos WHERE parentObjectId = :id AND oprName = :opr AND defectName =:defect")
    void updatePhotos(ArrayList<String> photos, int id, String opr, String defect);

    @Query("DELETE FROM defects WHERE parentObjectId = :id")
    void deleteDefectsByObject(int id);

    @Query("DELETE FROM defects WHERE parentObjectId = :id AND oprName = :opr AND defectName = :defect")
    void deleteCurtainDefect(int id, String opr, String defect);

    //ДОбавить проверку есть ли уже дефект в базе
}
