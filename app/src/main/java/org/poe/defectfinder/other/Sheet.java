package org.poe.defectfinder.other;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Sheet implements Serializable {

    @Override
    public String toString() {
        return "Sheet{" +
                "modificator='" + modificator + '\'' +
                ", tabel='" + tabel + '\'' +
                ", linename='" + linename + '\'' +
                ", tplnr='" + tplnr + '\'' +
                ", obhod='" + obhod + '\'' +
                ", type=" + type +
                ", highVoltage=" + highVoltage +
                ", opr=" + opr +
                ", time='" + time + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;
    private String modificator;
    private String tabel;
    private String linename;
    private String tplnr;
    private String obhod;
    private int type;//Линия 0,4, тп, линия 10
    private List<Opora> opr;
    private String time;
    private String status;
    private boolean highVoltage;
    private boolean thermalDoneOk;
    private boolean defectsFound;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sheet sheet = (Sheet) o;
        return type == sheet.type &&
                highVoltage == sheet.highVoltage &&
                Objects.equals(modificator, sheet.modificator) &&
                Objects.equals(tabel, sheet.tabel) &&
                Objects.equals(linename, sheet.linename) &&
                Objects.equals(tplnr, sheet.tplnr) &&
                Objects.equals(obhod, sheet.obhod) &&
                Objects.equals(opr, sheet.opr) &&
                Objects.equals(time, sheet.time) &&
                Objects.equals(status, sheet.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modificator, tabel, linename, tplnr, obhod, type, highVoltage, opr, time, status);
    }

    public Sheet() {}

    public Sheet(String modificator, String tabel, String linename, String tplnr, String obhod, int type, ArrayList<Opora> opr, String time, String status) {
        this.modificator = modificator;
        this.tabel = tabel;
        this.linename = linename;
        this.tplnr = tplnr;
        this.obhod = obhod;
        this.type = type;
        this.opr = opr;
        this.time = time;
        this.status = status;
    }

    public Sheet(int id, String modificator, String tabel, String linename, String tplnr, String obhod, int type, ArrayList<Opora> opr, String time, String status) {
        this.id = id;
        this.modificator = modificator;
        this.tabel = tabel;
        this.linename = linename;
        this.tplnr = tplnr;
        this.obhod = obhod;
        this.type = type;
        this.opr = opr;
        this.time = time;
        this.status = status;
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

    public String getLinename() {
        return linename;
    }

    public void setLinename(String linename) {
        this.linename = linename;
    }

    public String getTplnr() {
        return tplnr;
    }

    public void setTplnr(String tplnr) {
        this.tplnr = tplnr;
    }

    public String getObhod() {
        return obhod;
    }

    public void setObhod(String obhod) {
        this.obhod = obhod;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Opora> getOpr() { return opr; }

    public void setOpr(List<Opora> opr) { this.opr = opr; }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public boolean isDefectsFound() {
        return defectsFound;
    }

    public void setDefectsFound(boolean defectsFound) {
        this.defectsFound = defectsFound;
    }
}
