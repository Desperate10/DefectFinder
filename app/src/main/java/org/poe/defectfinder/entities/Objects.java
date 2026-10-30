package org.poe.defectfinder.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "objects")
public class Objects {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String modificator;
    private String tabel;
    private String obhod;
    private String name;
    private int type;
    private String tplnr;
    private String status;
    private String date;
    private boolean highVoltage;
    private boolean thermalDoneOk;
    private boolean thermalNotDone;
    private boolean defectsFound;
    private boolean defectsAbsent;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }



    public Objects( String modificator, String tabel, String obhod, String name, int type, String tplnr, String status, boolean highVoltage) {
        this.modificator = modificator;
        this.tabel = tabel;
        this.obhod = obhod;
        this.name = name;
        this.type = type;
        this.tplnr = tplnr;
        this.status = status;
        this.highVoltage = highVoltage;
    }

    @Ignore
    public Objects(int id, String modificator, String tabel, String obhod, String name, int type, String tplnr, String status, boolean highVoltage) {
        this.id = id;
        this.modificator = modificator;
        this.tabel = tabel;
        this.obhod = obhod;
        this.name = name;
        this.type = type;
        this.tplnr = tplnr;
        this.status = status;
        this.highVoltage = highVoltage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModificator() {
        return modificator;
    }

    public void setModificator(String modificator) {
        this.modificator = modificator;
    }

    public String getTabel() {
        return tabel;
    }

    public void setTabel(String tabel) {
        this.tabel = tabel;
    }

    public String getObhod() {
        return obhod;
    }

    public void setObhod(String obhod) {
        this.obhod = obhod;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTplnr() {
        return tplnr;
    }

    public void setTplnr(String tplnr) {
        this.tplnr = tplnr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHighVoltage() {
        return highVoltage;
    }

    public void setHighVoltage(boolean highVoltage) {
        this.highVoltage = highVoltage;
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
