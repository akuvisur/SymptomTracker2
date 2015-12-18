package com.comag.aku.symptomtracker.data_syncronization;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;
import com.comag.aku.symptomtracker.services.NotificationService;

import java.io.File;
import java.util.HashMap;

/**
 * Created by aku on 10/12/15.
 */
public class SyncProvider extends ContentProvider {
    public static String AUTHORITY = "com.comag.aku.symptomtracker.provider";
    public static final int DATABASE_VERSION = 2;

    private static final int ADVERSE_EVENTS = 1;
    private static final int ADVERSE_EVENTS_TYPE = 2;
    private static final int NOTIFICATIONS = 3;
    private static final int NOTIFICATIONS_TYPE = 4;

    public static final String DATABASE_NAME = "plugin_symptomtracker.db";

    public static final String[] DATABASE_TABLES = {"adverse_events", "notifications"};
    public static final String[] TABLES_FIELDS = {
            AdverseEventData._ID + " integer primary key autoincrement," +
                    AdverseEventData.TIMESTAMP + " real default 0," +
                    AdverseEventData.DEVICE_ID + " text default ''," +
                    AdverseEventData.USER_ID + " text default ''," +
                    AdverseEventData.TRACKABLE_TYPE + " text default ''," +
                    AdverseEventData.TRACKABLE_KEY + " text default ''," +
                    AdverseEventData.TRACKABLE_FREQUENCY + " text default ''," +
                    AdverseEventData.TRACKABLE_FREQUENCY_VALUE + " integer default 0," +
                    AdverseEventData.INPUT + " text default ''," +
                    AdverseEventData.COMMENT + " text default ''," +
                    AdverseEventData.PICTURE + " blob," +
                    "UNIQUE (" + AdverseEventData.TRACKABLE_FREQUENCY + "," + AdverseEventData.TRACKABLE_FREQUENCY_VALUE + "," + AdverseEventData.DEVICE_ID + "," + AdverseEventData.TRACKABLE_KEY + ")"
            ,
            NotificationEventData._ID + " integer primary key autoincrement," +
                    NotificationEventData.TIMESTAMP + " real default 0," +
                    NotificationEventData.DEVICE_ID + " text default ''," +
                    NotificationEventData.USER_ID + " text default ''," +
                    NotificationEventData.NOTIFICATION_TYPE + " text default ''," +
                    NotificationEventData.VALUE + " text default ''," +
                    NotificationEventData.CONTEXT + " text default '{}'," +
                    "UNIQUE (" + NotificationEventData.TIMESTAMP + "," + NotificationEventData.DEVICE_ID + ")"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> notificationMap = null;
    private static HashMap<String, String> eventMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(NotificationService.getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen()) ) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }
    /**
     * Allow resetting the ContentProvider when updating/reinstalling AWARE
     */
    public static void resetDB( Context c ) {
        Log.d("AWARE", "Resetting " + DATABASE_NAME + "...");

        File db = new File(DATABASE_NAME);
        db.delete();
        databaseHelper = new DatabaseHelper( c, DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if( databaseHelper != null ) {
            database = databaseHelper.getWritableDatabase();
        }
    }

    @Override
    public boolean onCreate() {
        //AUTHORITY = AppHelpers.package_name + ".provider.plugin.example"; //make AUTHORITY dynamic
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], ADVERSE_EVENTS); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", ADVERSE_EVENTS_TYPE); //URI for a single record

        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], NOTIFICATIONS); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", NOTIFICATIONS); //URI for a single record

        eventMap = new HashMap<>();
        eventMap.put(AdverseEventData._ID, AdverseEventData._ID);
        eventMap.put(AdverseEventData.TIMESTAMP, AdverseEventData.TIMESTAMP);
        eventMap.put(AdverseEventData.DEVICE_ID, AdverseEventData.DEVICE_ID);
        eventMap.put(AdverseEventData.USER_ID, AdverseEventData.USER_ID);
        eventMap.put(AdverseEventData.TRACKABLE_TYPE, AdverseEventData.TRACKABLE_TYPE);
        eventMap.put(AdverseEventData.TRACKABLE_KEY, AdverseEventData.TRACKABLE_KEY);
        eventMap.put(AdverseEventData.TRACKABLE_FREQUENCY, AdverseEventData.TRACKABLE_FREQUENCY);
        eventMap.put(AdverseEventData.TRACKABLE_FREQUENCY_VALUE, AdverseEventData.TRACKABLE_FREQUENCY_VALUE);
        eventMap.put(AdverseEventData.INPUT, AdverseEventData.INPUT);
        eventMap.put(AdverseEventData.COMMENT, AdverseEventData.COMMENT);
        eventMap.put(AdverseEventData.PICTURE, AdverseEventData.PICTURE);

        notificationMap = new HashMap<>();
        notificationMap.put(NotificationEventData._ID, NotificationEventData._ID);
        notificationMap.put(NotificationEventData.TIMESTAMP, NotificationEventData.TIMESTAMP);
        notificationMap.put(NotificationEventData.DEVICE_ID, NotificationEventData.DEVICE_ID);
        notificationMap.put(NotificationEventData.USER_ID, NotificationEventData.USER_ID);
        notificationMap.put(NotificationEventData.NOTIFICATION_TYPE, NotificationEventData.NOTIFICATION_TYPE);
        notificationMap.put(NotificationEventData.VALUE, NotificationEventData.VALUE);
        notificationMap.put(NotificationEventData.CONTEXT, NotificationEventData.CONTEXT);

        return true; //let Android know that the database is ready to be used.
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case NOTIFICATIONS:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(notificationMap);
                break;
            case ADVERSE_EVENTS:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(eventMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(NotificationService.getContext().getApplicationContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG) Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTIFICATIONS:
                return NotificationEventData.CONTENT_TYPE;
            case NOTIFICATIONS_TYPE:
                return NotificationEventData.CONTENT_ITEM_TYPE;
            case ADVERSE_EVENTS:
                return AdverseEventData.CONTENT_TYPE;
            case ADVERSE_EVENTS_TYPE:
                return AdverseEventData.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        long _id;
        switch (sUriMatcher.match(uri)) {
            case NOTIFICATIONS:
                _id = database.insert(DATABASE_TABLES[1], null, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(NotificationEventData.CONTENT_URI, _id);
                    NotificationService.getContext().getApplicationContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            case ADVERSE_EVENTS:
                _id = database.insert(DATABASE_TABLES[0], null, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(AdverseEventData.CONTENT_URI, _id);
                    NotificationService.getContext().getApplicationContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case NOTIFICATIONS:
                count = database.delete(DATABASE_TABLES[1], selection,selectionArgs);
                break;
            case ADVERSE_EVENTS:
                count = database.delete(DATABASE_TABLES[0], selection,selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        NotificationService.getContext().getApplicationContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case NOTIFICATIONS:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;
            case ADVERSE_EVENTS:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        NotificationService.getContext().getApplicationContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    public static final class AdverseEventData implements BaseColumns {
        private AdverseEventData(){};
        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/adverse_events");

        /**
         * How your data collection is identified internally in Android (vnd.android.cursor.dir). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.symptomtracker";

        /**
         * How each row is identified individually internally in Android (vnd.android.cursor.item). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.symptomtracker";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String USER_ID = "user_id";
        public static final String TRACKABLE_TYPE = "trackable_type";
        public static final String TRACKABLE_KEY = "trackable_key";
        public static final String TRACKABLE_FREQUENCY = "trackable_frequency";
        public static final String TRACKABLE_FREQUENCY_VALUE = "trackable_frequency_value";
        public static final String INPUT = "input";
        public static final String COMMENT = "comment";
        public static final String PICTURE = "picture";

   }

    public static final class NotificationEventData implements BaseColumns {
        private NotificationEventData(){};
        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notifications");

        /**
         * How your data collection is identified internally in Android (vnd.android.cursor.dir). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.symptomtracker";

        /**
         * How each row is identified individually internally in Android (vnd.android.cursor.item). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.symptomtracker";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String USER_ID = "user_id";
        public static final String NOTIFICATION_TYPE = "notification_type";
        public static final String VALUE = "value";
        public static final String CONTEXT = "context";

    }

}
