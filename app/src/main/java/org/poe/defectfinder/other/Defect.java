package org.poe.defectfinder.other;

import java.io.Serializable;
import java.util.ArrayList;

public class Defect implements Serializable {

    private String defect;
    private ArrayList<String> photo;
    private String note;
    private boolean thermalDoneOk;
    private boolean thermalNotDone;
    private boolean defectsFound;
    private boolean defectsAbsent;

    public Defect() {}

    public Defect(String defect, ArrayList<String> photo, String note) {
        this.defect = defect;
        this.photo = photo;
        this.note = note;
    }

    public Defect(String defect, ArrayList<String> photo, String note, boolean thermalDoneOk, boolean thermalNotDone) {
        this.defect = defect;
        this.photo = photo;
        this.note = note;
        this.thermalDoneOk = thermalDoneOk;
        this.thermalNotDone = thermalNotDone;
    }

    public Defect(String defect, ArrayList<String> photo, String note, boolean thermalDoneOk, boolean thermalNotDone, boolean defectsFound, boolean defectsAbsent) {
        this.defect = defect;
        this.photo = photo;
        this.note = note;
        this.thermalDoneOk = thermalDoneOk;
        this.thermalNotDone = thermalNotDone;
        this.defectsFound = defectsFound;
        this.defectsAbsent = defectsAbsent;
    }

    public ArrayList<String> getPhoto() {
        return photo;
    }

    public void setPhoto(ArrayList<String> photo) {
        this.photo = photo;
    }

    public String getDefect() {
        return defect;
    }

    public void setDefect(String defect) {
        this.defect = defect;
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
