package org.fitchfamily.android.gsmlocation.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

public final class SourceConnection {
    private HttpURLConnection connection;

    private Source source;
    private InputStream inputStream;

    public SourceConnection(Source source) throws IOException {
        this.source = source;

        final Iterator<String> urls = source.urls().iterator();

        inputStream = new SequenceInputStream(new Enumeration<InputStream>() {
            @Override
            public boolean hasMoreElements() {
                return urls.hasNext();
            }

            @Override
            public InputStream nextElement() {
                try {
                    URL url = new URL(urls.next());

                    if (url.getProtocol().equals("https")) {
                        connection = (HttpsURLConnection) url.openConnection();
                    } else {
                        connection = (HttpURLConnection) url.openConnection();
                    }

                    connection.setRequestMethod("GET");
                    connection.connect();

                    return connection.getInputStream();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        inputStream = new BufferedInputStream(inputStream);

        if (source.compression() == Source.Compression.gzip) {
            inputStream = new BufferedInputStream(
                    new GZIPInputStream(
                            inputStream
                    )
            );
        }
    }

    public int getCompressedContentLength() {
        return source.urls().size() == 1 ? connection.getContentLength() : -1;
    }

    public int getContentLength() {
        // Looks like .gz is about a 4 to 1 compression ratio
        final int length = connection.getContentLength();
        return length == -1 ? -1 : length * (source.compression() == Source.Compression.gzip ? 4 : 1);
    }

    public InputStream inputStream() {
        return inputStream;
    }
}
