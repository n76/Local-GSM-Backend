package org.fitchfamily.android.gsmlocation.async;

import android.content.Context;
import android.text.TextUtils;

import com.octo.android.robospice.request.SpiceRequest;

import org.fitchfamily.android.gsmlocation.Config;
import org.fitchfamily.android.gsmlocation.Settings;
import org.fitchfamily.android.gsmlocation.services.opencellid.OpenCellId;
import org.fitchfamily.android.gsmlocation.services.opencellid.OpenCellIdLimitException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ObtainOpenCellIdKeySpiceRequest extends SpiceRequest<ObtainOpenCellIdKeySpiceRequest.Result> {
    public static final String CACHE_KEY = "ObtainOpenCellIdKeySpiceRequest";

    private final Context context;

    public ObtainOpenCellIdKeySpiceRequest(Context context) {
        super(Result.class);
        this.context = context.getApplicationContext();
    }

    @Override
    public Result loadDataFromNetwork() throws Exception {
        if (!TextUtils.isEmpty(Settings.with(context).openCellIdApiKey())) {
            // don't request a new one
            return null;
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) new URL(Config.OCI_API_GET_KEY_URL).openConnection();
            conn.connect();
            int statusCode = conn.getResponseCode();

            if (statusCode == 200) {
                byte[] buf = new byte[36];

                if (conn.getInputStream().read(buf, 0, 36) != 36) {
                    throw new IOException("wrong content length");
                } else {
                    String newKey = new String(buf, "UTF-8");
                    OpenCellId.throwIfApiKeyInvalid(newKey);
                    Settings.with(context).openCellIdApiKey(newKey);

                    return null;
                }
            } else if (statusCode == 503) {
                throw new OpenCellIdLimitException();
            } else {
                throw new IOException("unexpected response code: " + statusCode);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static final class Result {

    }
}
