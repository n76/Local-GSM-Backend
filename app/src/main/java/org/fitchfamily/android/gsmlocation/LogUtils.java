package org.fitchfamily.android.gsmlocation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogUtils {
    private static final String LOG_PREFIX = "gsmloc_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 25;

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        } else {
            return LOG_PREFIX + str;
        }
    }

    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }

    public static void appendToLog(String message) {
        if (!Config.GEN_LOG_FILE.exists()) {
            try {
                Config.GEN_LOG_FILE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(Config.GEN_LOG_FILE, true));
            buf.append(message);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearLog() {
        Config.GEN_LOG_FILE.delete();
    }

    private LogUtils() {
    }
}
