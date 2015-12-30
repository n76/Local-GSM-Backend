package org.fitchfamily.android.gsmlocation.database;

/**
 * Used internally for caching. HashMap compatible entity class.
 */
class QueryArgs {
    private Integer mcc;

    private Integer mnc;

    private int cid;

    private int lac;

    QueryArgs(Integer mcc, Integer mnc, int cid, int lac) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.cid = cid;
        this.lac = lac;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        QueryArgs queryArgs = (QueryArgs) o;

        if (cid != queryArgs.cid)
            return false;

        if (lac != queryArgs.lac)
            return false;

        if (mcc != null ? !mcc.equals(queryArgs.mcc) : queryArgs.mcc != null)
            return false;

        if (mnc != null ? !mnc.equals(queryArgs.mnc) : queryArgs.mnc != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = mcc != null ? mcc.hashCode() : (1 << 16);
        result = 31 * result + (mnc != null ? mnc.hashCode() : (1 << 16));
        result = 31 * result + cid;
        result = 31 * result + lac;
        return result;
    }

    public String toString() {
        return "mcc=" + mcc + ", mnc=" + mnc + ", lac=" + lac + ", cid=" + cid;
    }
}
