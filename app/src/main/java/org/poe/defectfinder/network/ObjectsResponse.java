package org.poe.defectfinder.network;

public class ObjectsResponse {

    String tplnr;
    String pltxt;
    boolean highVoltage;

    ObjectsResponse() {}

    @Override
    public String toString() {
        return "ObjectResponse{" +
                "tplnr='" + tplnr + '\'' +
                ", pltxt='" + pltxt + '\'' +
                ", highVoltage=" + highVoltage +
                '}';
    }

    public String getTplnr() {
        return tplnr;
    }

    public void setTplnr(String tplnr) {
        this.tplnr = tplnr;
    }

    public String getPltxt() {
        return pltxt;
    }

    public void setPltxt(String pltxt) {
        this.pltxt = pltxt;
    }

    public boolean isHighVoltage() {
        return highVoltage;
    }

    public void setHighVoltage(boolean highVoltage) {
        this.highVoltage = highVoltage;
    }

    /** Last 4 characters of tplnr ("PO-F-04-P0052" -> "0052").
     *  Used as a short disambiguator in dropdowns instead of inventory number. */
    public String getTplnrSuffix() {
        if (tplnr == null) return "";
        int len = tplnr.length();
        return len <= 4 ? tplnr : tplnr.substring(len - 4);
    }
}
