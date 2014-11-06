package org.fitchfamily.android.gsmlocation.model;

public class CellInfo {
    private int MCC = -1;
    private int MNC = -1;
    private int CID = -1;
    private int LAC = -1;
    public int dbm = 0;
    private double lat = 0d;
    private double lng = 0d;
    private double rng = 0d;
    public long measurement;
    public long seen = System.currentTimeMillis();

    public CellInfo(int mcc, int mnc, int lac, int cid, double latV, double lon) {
        MCC = mcc;
        MNC = mnc;
        LAC = lac;
        CID = cid;
        dbm = 0;
        lat = latV;
        lng = lon;
        rng = 0;
        seen = System.currentTimeMillis();
    }

    public void setRng(double range_est) {
        rng = range_est;
        if (range_est < 800d) {
            rng = 800d;
        }
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getRng() {
        if (rng == 0)
            return 800d;
        else
            return rng;
    }

    public void setMeasurement(long meas) {
        measurement = meas;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CellInfo cellInfo = (CellInfo) o;

        if (CID != cellInfo.CID) return false;
        if (LAC != cellInfo.LAC) return false;
        if (MCC != cellInfo.MCC) return false;
        if (MNC != cellInfo.MNC) return false;
        if (Double.compare(cellInfo.lat, lat) != 0) return false;
        if (Double.compare(cellInfo.lng, lng) != 0) return false;

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        result = MCC;
        result = 31 * result + MNC;
        result = 31 * result + CID;
        result = 31 * result + LAC;
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public void sanitize() {
        if (MCC == Integer.MAX_VALUE) MCC = -1;
        if (MNC == Integer.MAX_VALUE) MNC = -1;
        if (CID == Integer.MAX_VALUE) CID = -1;
        if (LAC == Integer.MAX_VALUE) LAC = -1;
    }

    public boolean isInvalid() {
        return CID == -1 && LAC == -1;
    }

    public String toString() {
        return "CellInfo(" +
                "MCC=" + MCC +
                ", MNC=" + MNC +
                ", CID=" + CID +
                ", LAC=" + LAC +
                ", dbm=" + dbm +
                ", lng=" + lng +
                ", lat=" + lat +
                ", rng=" + rng +
                ')';
    }


}
