package org.fitchfamily.android.gsmlocation;

import android.os.Environment;

import java.io.File;

public class Config {
    public static final boolean DEBUG = BuildConfig.DEBUG;

    // Location of database
    public static final String DB_NAME = "lacells.db";
    public static final String DB_BAK_NAME = DB_NAME + ".bak";
    public static final String DB_NEW_NAME = DB_NAME + ".new";

    public static final File ROOT_DIR = Environment.getExternalStorageDirectory();
    public static final File DB_DIR = new File(ROOT_DIR, ".nogapps");
    public static final File DB_FILE = new File(DB_DIR, DB_NAME);
    public static final File DB_BAK_FILE = new File(DB_DIR, DB_BAK_NAME);
    public static final File DB_NEW_FILE = new File(DB_DIR, DB_NEW_NAME);

    public static final File GEN_LOG_FILE = new File(DB_DIR, "lacells_gen.log");

    // Strings for building URLs
    // Open Cell ID uses:
    // "http://opencellid.org/downloads/?apiKey=${API_KEY}&filename=cell_towers.csv.gz"
    public static final String OCI_URL_FMT = "http://opencellid.org/downloads/?apiKey=%s&filename=cell_towers.csv.gz";

    // URL for requesting new OpenCellID API key
    public static final String OCI_API_GET_KEY_URL = "http://opencellid.org/gsmCell/user/generateApiKey";

    // Mozilla Location Services uses:
    // "https://d17pt8qph6ncyq.cloudfront.net/export/MLS-full-cell-export-${NOW}T000000.csv.gz"
    public static final String MLS_URL_FMT = "https://d17pt8qph6ncyq.cloudfront.net/export/MLS-full-cell-export-%sT000000.csv.gz";

    public static final int MIN_RANGE = 500;
    public static final int MAX_RANGE = 100000;

    public static final String ABOUT_URL = "https://github.com/n76/Local-GSM-Backend/blob/master/README.md";
}
