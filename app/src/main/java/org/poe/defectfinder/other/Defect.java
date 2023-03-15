package org.poe.defectfinder.other;

import java.io.Serializable;
import java.util.ArrayList;

public class Defect implements Serializable {

    private String defect;
    private ArrayList<String> photo;
    private String note;

    public Defect() {}

    public Defect(String defect, ArrayList<String> photo, String note) {
        this.defect = defect;
        this.photo = photo;
        this.note = note;
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

}
