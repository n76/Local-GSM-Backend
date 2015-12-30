package org.fitchfamily.android.gsmlocation.database;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to simplify building SQL query conditions
 */
public class SqlWhereBuilder {
    private String delimiter;
    private StringBuilder query = new StringBuilder();
    private List<String> queryArgs = new ArrayList<>();

    /**
     * @return the current SQL query
     */
    public String selection() {
        return query.toString();
    }

    /**
     * @return the current SQL query arguments
     */
    public String[] selectionArgs() {
        String[] queryArgs = new String[this.queryArgs.size()];
        this.queryArgs.toArray(queryArgs);

        return queryArgs;
    }

    /**
     * Adds an delimiter. It will be appended before the next condition will be added to the request.
     * If you call this if an delimiter is set, the previous one will be discard.
     * @param delimiter the delimiter
     * @return this object
     */
    private SqlWhereBuilder delim(String delimiter) {
        this.delimiter = " " + delimiter + " ";
        return this;
    }

    /**
     * Sets the delimiter to "AND"
     * @return this object
     */
    public SqlWhereBuilder and() {
        return delim("AND");
    }

    /**
     * Ands and equal condition to the query.
     * @param column the column for the match
     * @param value the expected value
     * @return this object
     */
    public SqlWhereBuilder columnIs(String column, String value) {
        return appendDelimiter()
                .append(column + " = ?", value);
    }

    /**
     * Appends a delimiter if one is set
     * @return this object
     */
    private SqlWhereBuilder appendDelimiter() {
        if(!TextUtils.isEmpty(delimiter)) {
            append(delimiter);
            delimiter = null;
        }

        return this;
    }

    /**
     * Appends to the query
     * @param query an string to append to the query
     * @param args query arguments to append
     * @return this object
     */
    private SqlWhereBuilder append(String query, String... args) {
        this.query.append(query);
        this.queryArgs.addAll(Arrays.asList(args));

        return this;
    }
}
