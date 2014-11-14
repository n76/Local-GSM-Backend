package org.fitchfamily.android.gsmlocation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import java.util.Date;
import android.util.Log;

public class settings extends Activity implements View.OnClickListener {
    protected String TAG = "gsm-backend-settings";
    private static boolean DEBUG = true;

    Button btn;
    CheckBox useOCI;
    CheckBox useMLS;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Starting settings");
        setContentView(R.layout.settings);

        btn = (Button) findViewById(R.id.downloadButton);
        useOCI = (CheckBox)findViewById(R.id.useOCI);
        useMLS = (CheckBox)findViewById(R.id.useMLS);

        btn.setOnClickListener(this);
    }

    public void onClick(View view) {
        downloadData();
    }

    private void downloadData() {
//        btn.setText(new Date().toString());
//        Log.d(TAG, "updating time");

        String lg = "";
        if (useOCI.isChecked())
            lg += "Use OCI.  ";
        else
            lg += "Don't use OCI.  ";
        if (useMLS.isChecked())
            lg += "Use MLS.  ";
        else
            lg += "Don't use MLS.  ";
        if (DEBUG) Log.d(TAG, lg);
    }
}

