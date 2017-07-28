package org.fitchfamily.android.gsmlocation.services.opencellid;

public abstract class OpenCellId {
    private OpenCellId() {

    }

    /**
     * OpenCellID API keys appear to be canonical form version 4 UUIDs, except keys
     * generated with http://opencellid.org/gsmCell/user/generateApiKey have
     * "dev-usr-" instead of hexadecimal digits before the first hyphen.
     *
     * OCID is now being run by a new owner. Old keys appear to still work but new keys
     * seem to be hexidecimal numbers with 14 places.
     */
    public static boolean isApiKeyValid(String key) {
        // Old style keys
        return  key.matches(
                "(?:[0-9a-f]{8}|dev-usr-)-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") ||
                key.matches(("(?:[0-9a-f]{14})"));       // New style keys
    }

    public static void throwIfApiKeyInvalid(String key) {
        if (!isApiKeyValid(key)) {
            throw new InvalidOpenCellIdException();
        }
    }
}
