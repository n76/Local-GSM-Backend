package org.fitchfamily.android.gsmlocation;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by Marvin Arnold on 7/06/15.
 */
public class OcidResponse {
    private final HttpResponse httpResponse;

    public OcidResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public int getStatusCode() {
        return getStatus().getStatusCode();
    }

    public String getResponseFromServer() throws Exception {
        String responseFromServer = null;
        if (httpResponse.getEntity() != null) {
            InputStream is = httpResponse.getEntity().getContent();
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            // Read response into a buffered stream
            int readBytes;
            byte[] sBuffer = new byte[4096];
            while ((readBytes = is.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            responseFromServer = content.toString("UTF-8");
            httpResponse.getEntity().consumeContent();
        }

        return responseFromServer;
    }

    public StatusLine getStatus() {
        return httpResponse.getStatusLine();
    }
}