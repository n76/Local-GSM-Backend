package org.fitchfamily.android.gsmlocation.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Source {
    private final List<String> urls;
    private final Compression compression;

    public Source(List<String> urls, Compression compression) {
        if (urls == null || urls.isEmpty() || compression == null) {
            throw new NullPointerException();
        }

        this.urls = Collections.unmodifiableList(urls);
        this.compression = compression;
    }

    public Source(String url, Compression compression) {
        this(Arrays.asList(url), compression);
    }

    public Compression compression() {
        return compression;
    }

    public List<String> urls() {
        return urls;
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
}
