package org.fitchfamily.android.gsmlocation.async;

import java.util.Collections;
import java.util.List;

public class MccDetails {
    private final int numberOfRecords;
    private final List<String> urls;

    public MccDetails(int numberOfRecords, List<String> urls) {
        this.numberOfRecords = numberOfRecords;
        this.urls = Collections.unmodifiableList(urls);
    }

    public List<String> urls() {
        return urls;
    }

    public int numberOfRecords() {
        return numberOfRecords;
    }
}
