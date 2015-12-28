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

    private Source source;
    private InputStream inputStream;

    public SourceConnection(Source source) throws IOException {
        this.source = source;

        URL url = new URL(source.url());

        if (url.getProtocol().equals("https")) {
            connection = (HttpsURLConnection) url.openConnection();
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }

        connection.setRequestMethod("GET");
        connection.connect();

        inputStream = new BufferedInputStream(connection.getInputStream());

        if (source.compression() == Source.Compression.gzip) {
            inputStream = new BufferedInputStream(
                    new GZIPInputStream(
                            inputStream
                    )
            );
        }
    }

    public int getCompressedContentLength() {
        return connection.getContentLength();
    }

    public int getContentLength() {
        // Looks like .gz is about a 4 to 1 compression ratio
        return connection.getContentLength() * (source.compression() == Source.Compression.gzip ? 4 : 1);
    }

    public InputStream inputStream() {
        return inputStream;
    }
}
