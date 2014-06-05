package org.rexo.extraction;

/**
 * Created by klimzaporojets on 5/29/14.
 */
public class BoxCoordinates {
    private double ury;
    private double urx;
    private double lly;
    private double llx;
    private int pageNum;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public double getUry() {
        return ury;
    }

    public void setUry(double ury) {
        this.ury = ury;
    }

    public double getUrx() {
        return urx;
    }

    public void setUrx(double urx) {
        this.urx = urx;
    }

    public double getLly() {
        return lly;
    }

    public void setLly(double lly) {
        this.lly = lly;
    }

    public double getLlx() {
        return llx;
    }

    public void setLlx(double llx) {
        this.llx = llx;
    }
}
