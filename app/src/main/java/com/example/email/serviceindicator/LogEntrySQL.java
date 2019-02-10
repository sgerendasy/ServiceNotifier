package com.example.email.serviceindicator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class LogEntrySQL extends SQLiteOpenHelper
{
    private static final String TAG = LogEntrySQL.class.getSimpleName();

    private static final int DATABASE_VERSION = 1;
    private static final String LOG_LIST_TABLE = "service_log_entries";
    private static final String DATABASE_NAME = "serviceLogs";

    static final String KEY_ID = "_id";
    static final String DATETIME = "datetime";
    static final String CONNECTION_INFO = "conn_info";


    // Create table query
    private static final String LOG_LIST_TABLE_CREATE =
            "CREATE TABLE " + LOG_LIST_TABLE + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY, " +
                    DATETIME + " DATETIME, " +
                    CONNECTION_INFO + " TEXT);";


    private SQLiteDatabase writableDB;
    private SQLiteDatabase readableDB;


    public LogEntrySQL(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        readableDB = getReadableDatabase();
        writableDB = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LOG_LIST_TABLE_CREATE);
    }

    public void addEntry(LogEntry newLogEntry)
    {
        // create a container for the data
        ContentValues values = new ContentValues();
        values.put(DATETIME, newLogEntry.dateTime);
        values.put(CONNECTION_INFO, newLogEntry.connectionInfo);
        writableDB.insert(LOG_LIST_TABLE, null, values);
    }

    public ArrayList<LogEntry> getLogs() {
        ArrayList<LogEntry> logs = new ArrayList<>();
        String query = "SELECT * FROM service_log_entries sle ORDER BY sle.datetime DESC";
        Cursor cursor = readableDB.rawQuery(query, null);
        LogEntry newLogEntry;
        if (cursor.getCount() >= 1) {
            while (cursor.moveToNext()) {
                newLogEntry = new LogEntry(cursor.getString(1), cursor.getString(2));
                logs.add(newLogEntry);
            }
        }
        cursor.close();
        return logs;
    }

    public int ClearLogs()
    {
//        writableDB.execSQL("delete from " + LOG_LIST_TABLE);
        return writableDB.delete(LOG_LIST_TABLE, "1", null);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

    }
}
