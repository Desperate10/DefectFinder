package org.poe.defectfinder.other;

public class DefectResponse {

    private String iwerk;
    private String tplnr;
    private String filial;
    private String pltxt;
    private String codedefect;
    private String codeplace;

    @Override
    public String toString() {
        return "DefectResponse{" +
                "iwerk='" + iwerk + '\'' +
                ", tplnr='" + tplnr + '\'' +
                ", filial='" + filial + '\'' +
                ", pltxt='" + pltxt + '\'' +
                ", codedefect='" + codedefect + '\'' +
                ", codeplace='" + codeplace + '\'' +
                '}';
    }

    public String getIwerk() {
        return iwerk;
    }

    public void setIwerk(String iwerk) {
        this.iwerk = iwerk;
    }

    public String getTplnr() {
        return tplnr;
    }

    public void setTplnr(String tplnr) {
        this.tplnr = tplnr;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getPltxt() {
        return pltxt;
    }

    public void setPltxt(String pltxt) {
        this.pltxt = pltxt;
    }

    public String getCodedefect() {
        return codedefect;
    }

    public void setCodedefect(String codedefect) {
        this.codedefect = codedefect;
    }

    public String getCodeplace() {
        return codeplace;
    }

    public void setCodeplace(String codeplace) {
        this.codeplace = codeplace;
    }

    public DefectResponse(String iwerk, String tplnr, String filial, String pltxt, String codedefect, String codeplace) {
        this.iwerk = iwerk;
        this.tplnr = tplnr;
        this.filial = filial;
        this.pltxt = pltxt;
        this.codedefect = codedefect;
        this.codeplace = codeplace;
    }

    public DefectResponse(){}
}
