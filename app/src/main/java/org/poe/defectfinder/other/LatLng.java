package org.poe.defectfinder.other;

import android.location.Location;

import java.io.Serializable;

public class LatLng implements Serializable {

    private String invent;
    private String linename;
    private String opr;
    private Location location;

    public LatLng(String invent, String linename, Location location) {
        this.invent = invent;
        this.linename = linename;
        this.location = location;
    }

    public LatLng(String invent, String linename, String opr, Location location) {
        this.invent = invent;
        this.linename = linename;
        this.opr = opr;
        this.location = location;
    }

    public String getInvent() {
        return invent;
    }

    public void setInvent(String invent) {
        this.invent = invent;
    }

    public String getLinename() {
        return linename;
    }

    public void setLinename(String linename) {
        this.linename = linename;
    }

    public String getOpr() {
        return opr;
    }

    public void setOpr(String opr) {
        this.opr = opr;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }



}
