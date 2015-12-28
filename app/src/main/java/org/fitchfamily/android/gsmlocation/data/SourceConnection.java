package org.fitchfamily.android.gsmlocation.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

public final class SourceConnection {
    private HttpURLConnection connection;

    private InputStream inputStream;

    public SourceConnection(Source source) throws IOException {
        URL u = new URL(source.url());

        if (u.getProtocol().equals("https")) {
            connection = (HttpsURLConnection) u.openConnection();
        } else {
            connection = (HttpURLConnection) u.openConnection();
        }

        connection.setRequestMethod("GET");
        connection.connect();

        inputStream = new BufferedInputStream(
                new GZIPInputStream(
                        new BufferedInputStream(
                                connection.getInputStream())));
    }

    public int getCompressedContentLength() {
        return connection.getContentLength();
    }

    public int getContentLength() {
        // Looks like .gz is about a 4 to 1 compression ratio
        return connection.getContentLength() * 4;
    }

    public InputStream inputStream() {
        return inputStream;
    }
}
