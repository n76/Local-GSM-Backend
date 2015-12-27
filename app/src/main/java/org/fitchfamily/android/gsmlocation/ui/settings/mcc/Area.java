package org.fitchfamily.android.gsmlocation.ui.settings.mcc;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Set;

public class Area implements Comparable<Area> {
    private final String label;

    private final Set<Integer> mccs;

    private final String code;

    private Status status;

    public Area(String code, String label, Status status, Set<Integer> mccs) {
        this.mccs = Collections.unmodifiableSet(mccs);
        this.code = code;
        this.status = status;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public Status status() {
        return status;
    }

    public String code() {
        return code;
    }

    public void status(Status status) {
        this.status = status;
    }

    public Set<Integer> mmcs() {
        return mccs;
    }

    @Override
    public int compareTo(@NonNull Area another) {
        return label.compareTo(another.label);
    }

    public enum Status {
        enabled, disabled, mixed
    }
}
