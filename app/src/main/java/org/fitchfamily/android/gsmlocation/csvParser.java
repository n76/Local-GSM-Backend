package org.fitchfamily.android.gsmlocation;

import java.util.List;
import java.util.Vector;
import java.io.InputStream;

public class csvParser {
    protected String TAG = appConstants.TAG_PREFIX+"csvParser";
    private static boolean DEBUG = appConstants.DEBUG;

    private InputStream r = null;

    private int chCount = 0;

    public csvParser(InputStream r) {
        this.r = r;
    }

    public int bytesRead() {
        return chCount;
    }

    public List parseLine() throws Exception {
        return parseLine(r);
    }
    /**
    * Returns a null when the input stream is empty
    */
    public List parseLine(InputStream r) throws Exception {
        int ch = r.read();
        chCount++;
        while (ch == '\r') {
            ch = r.read();
            chCount++;
        }
        if (ch<0) {
            return null;
        }
        Vector store = new Vector();
        StringBuffer curVal = new StringBuffer();
        boolean inquotes = false;
        boolean started = false;
        while (ch>=0) {
            if (inquotes) {
                started=true;
                if (ch == '\"') {
                    inquotes = false;
                }
                else {
                    curVal.append((char)ch);
                }
            }
            else {
                if (ch == '\"') {
                    inquotes = true;
                    if (started) {
   // if this is the second quote in a value, add a quote
   // this is for the double quote in the middle of a value
                        curVal.append('\"');
                    }
                }
                else if (ch == ',') {
                    store.add(curVal.toString());
                    curVal = new StringBuffer();
                    started = false;
                }
                else if (ch == '\r') {
                    //ignore LF characters
                }
                else if (ch == '\n') {
                    //end of a line, break out
                    break;
                }
                else {
                    curVal.append((char)ch);
                }
            }
            ch = r.read();
            chCount++;
        }
        store.add(curVal.toString());
        return store;
    }
}
