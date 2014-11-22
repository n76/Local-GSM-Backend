package org.fitchfamily.android.gsmlocation;

import java.io.File;
import android.os.Environment;

class appConstants {

    // Logging related values
    public static final String TAG_PREFIX = "gsm-backend-";
    public static boolean DEBUG = true;

    // Location of database
    public static final File ROOT_DIR = Environment.getExternalStorageDirectory();
    public static final File DB_DIR = new File(ROOT_DIR, ".microG");
    public static final String DB_NAME = "lacells.db";
    public static final File DB_FILE = new File(DB_DIR, DB_NAME);
    public static final String DB_BAK_NAME = DB_NAME + ".bak";
    public static final File DB_BAK_FILE = new File(DB_DIR, DB_NAME);

    public static final File OLD_DB_DIR = new File(ROOT_DIR, ".nogapps");
    public static final File OLD_DB_FILE = new File(OLD_DB_DIR, DB_NAME);

    // Strings for building URLs
    // Open Cell ID uses:
    // "http://opencellid.org/downloads/?apiKey=${API_KEY}&filename=cell_towers.csv.gz"
    public static final String OCI_URL_PREFIX = "http://opencellid.org/downloads/?apiKey=";
    public static final String OCI_URL_SUFFIX = "&filename=cell_towers.csv.gz";

    // Mozilla Location Services uses:
    // "https://d17pt8qph6ncyq.cloudfront.net/export/MLS-full-cell-export-${NOW}T000000.csv.gz"
    public static final String MLS_URL_PREFIX = "https://d17pt8qph6ncyq.cloudfront.net/export/MLS-full-cell-export-";
    public static final String MLS_URL_SUFFIX = "T000000.csv.gz";

    public static final int MIN_RANGE = 500;
    public static final int MAX_RANGE = 100000;
}
