package org.poe.defectfinder;

import java.util.ArrayList;
import java.util.List;

public class Line {

    private int id;
    private int pid;
    private int multi;
    private String invent;
    private String tplnr;
    private String name;
    List<Mast> mast = new ArrayList<>();

    public Line() {}

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }
    public int getPid() {
        return pid;
    }

    public void setMulti(int multi) {
        this.multi = multi;
    }
    public int getMulti() {
        return multi;
    }

    public void setInvent(String invent) {
        this.invent = invent;
    }
    public String getInvent() {
        return invent;
    }

    public void setTplnr(String tplnr) {
        this.tplnr = tplnr;
    }
    public String getTplnr() {
        return tplnr;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setMast(List<Mast> mast) {
        this.mast = mast;
    }
    public List<Mast> getMast() {
        return mast;
    }

    public void delete(Line line) {

    }
}

class Mast {
    String opr;
    double lat;
    double lng;
    double alt;
    double acc;
    String wire;

    Mast() {}

    Mast(String opr, double lat, double lng) {
        this.opr = opr;
        this.lat = lat;
        this.lng = lng;
    }

    public void setOpr(String opr) {
        this.opr = opr;
    }
    public String getOpr() {
        return opr;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }
    public double getLat() {
        return lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
    public double getLng() {
        return lng;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }
    public double getAlt() {
        return alt;
    }

    public void setAcc(double acc) {
        this.acc = acc;
    }
    public double getAcc() {
        return acc;
    }

    public void setWire(String wire) {
        this.wire = wire;
    }
    public String getWire() {
        return wire;
    }
}
