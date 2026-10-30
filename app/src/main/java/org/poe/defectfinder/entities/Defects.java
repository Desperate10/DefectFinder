package org.poe.defectfinder.entities;

import org.jetbrains.annotations.NotNull;
import org.poe.defectfinder.other.Converters;

import java.util.ArrayList;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "defects")
public class Defects {

    @PrimaryKey(autoGenerate = true)
    @NotNull
    private int id;
    private int parentObjectId;
    private String oprName;
    private int oprOrSpan;
    private String defectName;
    @TypeConverters({Converters.class})
    private ArrayList<String> photos;
    private String note;
    private String modificator;
    private boolean thermalDoneOk;
    private boolean thermalNotDone;
    private boolean defectsFound;
    private boolean defectsAbsent;

    public Defects(int parentObjectId, String oprName, int oprOrSpan, String defectName, ArrayList<String> photos, String note, String modificator) {
        this.parentObjectId = parentObjectId;
        this.oprName = oprName;
        this.oprOrSpan = oprOrSpan;
        this.defectName = defectName;
        this.photos = photos;
        this.note = note;
        this.modificator = modificator;
    }

    @Ignore
    public Defects(int parentObjectId, String oprName, int oprOrSpan, String defectName, ArrayList<String> photos, String note, String modificator, boolean thermalDoneOk, boolean thermalNotDone) {
        this.parentObjectId = parentObjectId;
        this.oprName = oprName;
        this.oprOrSpan = oprOrSpan;
        this.defectName = defectName;
        this.photos = photos;
        this.note = note;
        this.modificator = modificator;
        this.thermalDoneOk = thermalDoneOk;
        this.thermalNotDone = thermalNotDone;
    }

    @Ignore
    public Defects(int parentObjectId, String oprName, int oprOrSpan, String defectName, ArrayList<String> photos, String note, String modificator, boolean thermalDoneOk, boolean thermalNotDone, boolean defectsFound, boolean defectsAbsent) {
        this.parentObjectId = parentObjectId;
        this.oprName = oprName;
        this.oprOrSpan = oprOrSpan;
        this.defectName = defectName;
        this.photos = photos;
        this.note = note;
        this.modificator = modificator;
        this.thermalDoneOk = thermalDoneOk;
        this.thermalNotDone = thermalNotDone;
        this.defectsFound = defectsFound;
        this.defectsAbsent = defectsAbsent;
    }

    public String getModificator() {
        return modificator;
    }

    public void setModificator(String modificator) {
        this.modificator = modificator;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParentObjectId() {
        return parentObjectId;
    }

    public void setParentObjectId(int parentObjectId) {
        this.parentObjectId = parentObjectId;
    }

    public String getOprName() {
        return oprName;
    }

    public void setOprName(String oprName) {
        this.oprName = oprName;
    }

    public int getOprOrSpan() {
        return oprOrSpan;
    }

    public void setOprOrSpan(int oprOrSpan) {
        this.oprOrSpan = oprOrSpan;
    }

    public String getDefectName() {
        return defectName;
    }

    public void setDefectName(String defectName) {
        this.defectName = defectName;
    }

    public ArrayList<String> getPhotos() {
        return photos;
    }

    public void setPhotos(ArrayList<String> photos) {
        this.photos = photos;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isThermalDoneOk() {
        return thermalDoneOk;
    }

    public void setThermalDoneOk(boolean thermalDoneOk) {
        this.thermalDoneOk = thermalDoneOk;
    }

    public boolean isThermalNotDone() {
        return thermalNotDone;
    }

    public void setThermalNotDone(boolean thermalNotDone) {
        this.thermalNotDone = thermalNotDone;
    }

    public boolean isDefectsFound() {
        return defectsFound;
    }

    public void setDefectsFound(boolean defectsFound) {
        this.defectsFound = defectsFound;
    }

    public boolean isDefectsAbsent() {
        return defectsAbsent;
    }

    public void setDefectsAbsent(boolean defectsAbsent) {
        this.defectsAbsent = defectsAbsent;
    }

}
