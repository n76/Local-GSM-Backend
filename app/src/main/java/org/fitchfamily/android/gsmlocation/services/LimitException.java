package org.fitchfamily.android.gsmlocation.services;

public class LimitException extends RuntimeException {
    public LimitException() {
        super("a limit was reached");
    }

    public LimitException(String message) {
        super(message);
    }
}
