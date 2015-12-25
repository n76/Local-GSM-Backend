package org.fitchfamily.android.gsmlocation;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.io.IOException;

public class DatabaseCreator {
    private static final String SQL_INSERT = "INSERT INTO cells (mcc, mnc, lac, cid, longitude, latitude, accuracy, samples, altitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, -1);";

    public static DatabaseCreator withTempFile() throws IOException {
        return with(File.createTempFile("new_lacells", ".db", Config.DB_DIR));
    }

    public static DatabaseCreator with(File file) {
        return new DatabaseCreator(file);
    }

    private File file;
    private SQLiteDatabase database;
    private SQLiteStatement insertStatement;

    private DatabaseCreator(File file) {
        this.file = file;
    }

    /**
     * Opens the database, does nothing if the database is already opened
     * @return this object
     */
    public DatabaseCreator open() {
        if(database == null) {
            database = SQLiteDatabase.openDatabase(file.getAbsolutePath(),
                    null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS +
                            SQLiteDatabase.OPEN_READWRITE +
                            SQLiteDatabase.CREATE_IF_NECESSARY
            );
        }

        return this;
    }

    /**
     * @throws UnsupportedOperationException if the database isn't opened
     */
    private void ensureOpened() {
        if(database == null) {
            throw new UnsupportedOperationException(file + " is not opened");
        }
    }

    /**
     * @throws UnsupportedOperationException if the database is opened
     */
    private void ensureClosed() {
        if(database == null) {
            throw new UnsupportedOperationException(file + " is opened");
        }
    }

    /**
     * Creates the table
     * @throws UnsupportedOperationException if the database isn't opened
     * @return this object
     */
    public DatabaseCreator createTable() {
        ensureOpened();

        database.execSQL("CREATE TABLE cells(mcc INTEGER, mnc INTEGER, lac INTEGER, cid INTEGER, longitude REAL, latitude REAL, accuracy REAL, samples INTEGER, altitude REAL);");

        return this;
    }

    /**
     * Creates the index
     * @throws UnsupportedOperationException if the database isn't opened
     * @return this object
     */
    public DatabaseCreator createIndex() {
        ensureOpened();

        database.execSQL("CREATE INDEX _idx1 ON cells (mcc, mnc, lac, cid);");
        database.execSQL("CREATE INDEX _idx2 ON cells (lac, cid);");

        return this;
    }

    /**
     * Closes the database, does nothing if not opened
     * @return this object
     */
    public DatabaseCreator close() {
        if(database != null) {
            database.close();

            database = null;
            insertStatement = null;
        }

        return this;
    }

    /**
     * Tries to remove the journal file
     * @throws UnsupportedOperationException if the database is opened
     * @return this object
     */
    public DatabaseCreator removeJournal() {
        ensureClosed();

        final File journalFile = new File(file.getAbsolutePath() + "-journal");

        if (!journalFile.delete()) {
            // ignore the error
        }

        return this;
    }

    /**
     * Tries to delete the database and the journal file
     * @throws UnsupportedOperationException if the database is opened
     * @return this object
     */
    public DatabaseCreator delete() {
        ensureClosed();

        removeJournal();

        if(!file.delete()) {
            // ignore
        }

        return this;
    }

    /**
     * Tries to rename the database to the given file
     * @param file the new name
     * @throws UnsupportedOperationException if the database is opened
     * @return this object
     */
    public DatabaseCreator renameTo(File file) {
        ensureClosed();

        if(!this.file.renameTo(file)) {
            // ignore
        }
        this.file = file;

        return this;
    }

    /**
     * Tries to replace the given database
     * @param file the target file
     * @throws UnsupportedOperationException if the database is opened
     * @return this object
     */
    public DatabaseCreator replace(File file) {
        ensureClosed();

        file.delete();  // delete if exists

        renameTo(file);

        return this;
    }

    /**
     * start a transaction on the database
     * @throws UnsupportedOperationException if the database is not opened
     * @return this object
     */
    public DatabaseCreator beginTransaction() {
        ensureOpened();

        database.beginTransaction();

        return this;
    }

    /**
     * start a transaction on the database
     * @throws UnsupportedOperationException if the database is not opened
     * @return this object
     */
    public DatabaseCreator commitTransaction() {
        ensureOpened();

        database.setTransactionSuccessful();
        database.endTransaction();

        return this;
    }

    /**
     * @throws UnsupportedOperationException if the database is not opened
     * @return this object
     */
    public DatabaseCreator insert(int mcc, String mnc, String lac, String cid, String longitude, String latitude, String accuracy, String samples) {
        ensureOpened();

        if(insertStatement == null) {
            insertStatement = database.compileStatement(SQL_INSERT);
        }

        int range = Integer.parseInt(accuracy);
        range = Math.max(range, Config.MIN_RANGE);
        range = Math.min(range, Config.MAX_RANGE);

        insertStatement.bindString(1, Integer.toString(mcc));
        insertStatement.bindString(2, mnc);
        insertStatement.bindString(3, lac);
        insertStatement.bindString(4, cid);
        insertStatement.bindString(5, longitude);
        insertStatement.bindString(6, latitude);
        insertStatement.bindString(7, Integer.toString(range));
        insertStatement.bindString(8, samples);
        insertStatement.executeInsert();

        insertStatement.clearBindings();

        return this;
    }
}
