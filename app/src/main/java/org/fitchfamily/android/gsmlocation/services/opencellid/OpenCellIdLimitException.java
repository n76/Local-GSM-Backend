package org.fitchfamily.android.gsmlocation.services.opencellid;

import org.fitchfamily.android.gsmlocation.services.LimitException;

public class OpenCellIdLimitException extends LimitException {
    public OpenCellIdLimitException() {
        super("OpenCellId API limit reached");
    }
}
