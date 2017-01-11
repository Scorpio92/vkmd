package ru.scorpio92.kmd.Types;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.File;

/**
 * Created by scorpio92 on 09.10.16.
 */

public class MainDB extends SQLiteOpenHelper implements BaseColumns {

    public static String DB_PATH = null;
    private static final String DEFAULT_DB_FOLDER = System.getenv("EXTERNAL_STORAGE") + "/kmd/databases";
    private static final String MAIN_DATABASE_NAME = "main.db";
    private static final int MAIN_DATABASE_VERSION = 1;

    public static final String LOGIN_TABLE = "login_table";
    public static final String LOGIN_TABLE_VALUE_COLUMN = "value";
    public static final String LOGIN_TABLE_AUTO_ENTER_COLUMN = "auto_enter";

    public static final String MEDIA_TABLE = "media_table";
    public static final String MEDIA_TABLE_OWNER_ID_COLUMN = "owner_id";
    public static final String MEDIA_TABLE_TRACK_ID_COLUMN = "track_id";
    public static final String MEDIA_TABLE_ARTISTS_COLUMN = "artists";
    public static final String MEDIA_TABLE_TRACK_NAME_COLUMN = "track_name";
    public static final String MEDIA_TABLE_DURATION_COLUMN = "duration";
    public static final String MEDIA_TABLE_URL_COLUMN = "url";
    public static final String MEDIA_TABLE_NEED_DOWNLOAD_COLUMN = "need_download";
    public static final String MEDIA_TABLE_WAS_DOWNLOADED_COLUMN = "was_downloaded";
    public static final String MEDIA_TABLE_DOWNLOAD_PATH_COLUMN = "download_path";

    private static final String AUTH_TABLE_CREATE_SCRIPT = "create table "
            + LOGIN_TABLE + " ("
            + BaseColumns._ID  + " integer primary key autoincrement, "
            + LOGIN_TABLE_VALUE_COLUMN + " text not null, "
            + LOGIN_TABLE_AUTO_ENTER_COLUMN + " text not null);";


    private static final String MEDIA_TABLE_CREATE_SCRIPT = "create table "
            + MEDIA_TABLE + " ("
            + BaseColumns._ID  + " integer primary key autoincrement, "
            + MEDIA_TABLE_OWNER_ID_COLUMN + " text not null, "
            + MEDIA_TABLE_TRACK_ID_COLUMN + " text not null, "
            + MEDIA_TABLE_ARTISTS_COLUMN + " text not null, "
            + MEDIA_TABLE_TRACK_NAME_COLUMN + " text not null, "
            + MEDIA_TABLE_DURATION_COLUMN + " text not null, "
            + MEDIA_TABLE_URL_COLUMN + " text not null, "
            + MEDIA_TABLE_NEED_DOWNLOAD_COLUMN + " text not null, "
            + MEDIA_TABLE_WAS_DOWNLOADED_COLUMN + " text not null, "
            + MEDIA_TABLE_DOWNLOAD_PATH_COLUMN + " text);";



    public MainDB(Context context) {
        super(context, checkDBWorkDir(), null, MAIN_DATABASE_VERSION);
        Log.w("MainDB", "DB_PATH: " + DB_PATH);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(AUTH_TABLE_CREATE_SCRIPT);
        sqLiteDatabase.execSQL(MEDIA_TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF IT EXISTS " + LOGIN_TABLE);
        sqLiteDatabase.execSQL("DROP TABLE IF IT EXISTS " + MEDIA_TABLE);
        onCreate(sqLiteDatabase);
    }

    public static String checkDBWorkDir() {
        if(DB_PATH == null) {
            Log.w("MainDB", "checkDBWorkDir");
            File workDir = new File(DEFAULT_DB_FOLDER);
            if (workDir.exists()) {
                DB_PATH = DEFAULT_DB_FOLDER + "/" + MAIN_DATABASE_NAME;
                return DB_PATH;
            } else {
                if (workDir.mkdirs()) {
                    DB_PATH = DEFAULT_DB_FOLDER + "/" + MAIN_DATABASE_NAME;
                    return DB_PATH;
                } else {
                    DB_PATH = MAIN_DATABASE_NAME;
                    return DB_PATH;
                }
            }
        } else {
            return DB_PATH;
        }
    }
}
