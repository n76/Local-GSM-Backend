package org.fitchfamily.android.gsmlocation;

public class Config {
    public static final boolean DEBUG = BuildConfig.DEBUG;

    // Strings for building URLs
    // Open Cell ID uses:
    // "http://opencellid.org/downloads/?apiKey=${API_KEY}&filename=cell_towers.csv.gz"
    public static final String OCI_URL_FMT = "http://opencellid.org/downloads/?apiKey=%s&filename=cell_towers.csv.gz";

    // URL for requesting new OpenCellID API key
    public static final String OCI_API_GET_KEY_URL = "http://opencellid.org/gsmCell/user/generateApiKey";

    // URL for lacells
    public static final String LACELLS_MCC_URL = "https://wvengen.github.io/lacells/lacells-countries.csv";

    // Mozilla Location Services uses:
    // "https://d17pt8qph6ncyq.cloudfront.net/export/MLS-full-cell-export-${NOW}T000000.csv.gz"
    public static final String MLS_URL_FMT = "https://d17pt8qph6ncyq.cloudfront.net/export/MLS-full-cell-export-%sT000000.csv.gz";

    public static final int MIN_RANGE = 500;
    public static final int MAX_RANGE = 100000;

    public static final String ABOUT_URL = "https://github.com/n76/Local-GSM-Backend/blob/master/README.md";
}
