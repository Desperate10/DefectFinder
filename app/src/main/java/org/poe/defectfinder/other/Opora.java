package org.poe.defectfinder.other;

import java.io.Serializable;
import java.util.List;

public class Opora implements Serializable {

    private String opr;
    private int oprOrSpan;
    private List<Defect> defect;

    public String getOpr() {
        return opr;
    }

    public void setOpr(String opr) {
        this.opr = opr;
    }

    public int getOprOrSpan() {
        return oprOrSpan;
    }

    public void setOprOrSpan(int oprOrSpan) {
        this.oprOrSpan = oprOrSpan;
    }

    public List<Defect> getDefect() {
        return defect;
    }

    public void setDefect(List<Defect> defect) {
        this.defect = defect;
    }

    public Opora(String opr, int oprOrSpan, List<Defect> defect) {
        this.opr = opr;
        this.oprOrSpan = oprOrSpan;
        this.defect = defect;
    }

}
