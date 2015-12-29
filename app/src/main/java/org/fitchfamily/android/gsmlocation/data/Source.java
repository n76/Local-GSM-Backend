package org.fitchfamily.android.gsmlocation.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Source {
    public static final int UNKNOWN = -1;

    private final List<String> urls;
    private final Compression compression;

    private final int expectedRecords;

    public Source(List<String> urls, Compression compression, int expectedRecords) {
        if (urls == null || urls.isEmpty() || compression == null) {
            throw new NullPointerException();
        }

        this.urls = Collections.unmodifiableList(urls);
        this.compression = compression;
        this.expectedRecords = expectedRecords;
    }

    public Source(List<String> urls, Compression compression) {
        this(urls, compression, UNKNOWN);
    }

    public Source(String url, Compression compression, int expectedRecords) {
        this(Arrays.asList(url), compression, expectedRecords);
    }

    public Source(String url, Compression compression) {
        this(url, compression, UNKNOWN);
    }

    public Compression compression() {
        return compression;
    }

    public List<String> urls() {
        return urls;
    }

    public int expectedRecords() {
        return expectedRecords;
    }

    public SourceConnection connect() throws IOException {
        return new SourceConnection(this);
    }

    @Override
    public String toString() {
        return urls() + " (" + compression().name() + ")";
    }

    public enum Compression {
        gzip, none
    }

    public static long expectedRecords(List<Source> sources) {
        long expectedRecords = 0;

        for(Source source : sources) {
            if(source.expectedRecords() == UNKNOWN) {
                return UNKNOWN;
            } else {
                expectedRecords += source.expectedRecords();
            }
        }

        return expectedRecords;
    }
}
