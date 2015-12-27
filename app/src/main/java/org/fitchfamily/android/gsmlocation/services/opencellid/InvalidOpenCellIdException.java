package org.fitchfamily.android.gsmlocation.services.opencellid;

public class InvalidOpenCellIdException extends RuntimeException {
    public InvalidOpenCellIdException() {
        super("the provided OpenCellId is invalid");
    }
}
