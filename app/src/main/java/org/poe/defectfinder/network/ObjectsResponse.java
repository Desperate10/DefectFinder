package org.poe.defectfinder.network;

public class ObjectsResponse {

    String tplnr;
    String pltxt;
    String invnr;

    ObjectsResponse() {}

    @Override
    public String toString() {
        return "ObjectResponse{" +
                "tplnr='" + tplnr + '\'' +
                ", pltxt='" + pltxt + '\'' +
                ", invnr='" + invnr + '\'' +
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

    public String getInvnr() {
        return invnr;
    }

    public void setInvnr(String invnr) {
        this.invnr = invnr;
    }
}
