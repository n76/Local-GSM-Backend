package org.fitchfamily.android.gsmlocation;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogUtils {
    private static LogUtils instance;
    private static final Object lock = new Object();

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

    public static LogUtils with(Context context) {
        if(instance == null) {
            synchronized (lock) {
                if(instance == null) {
                    instance = new LogUtils(context);
                }
            }
        }

        return instance;
    }

    private Settings settings;

    private LogUtils(Context context) {
        settings = Settings.with(context);
    }

    public void appendToLog(String message) {
        if (!settings.logfile().exists()) {
            try {
                settings.logfile().createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(settings.logfile(), true));
            buf.append(message);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearLog() {
        settings.logfile().delete();
    }

    private LogUtils() {
    }
}
