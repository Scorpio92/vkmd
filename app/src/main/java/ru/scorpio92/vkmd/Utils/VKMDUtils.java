package ru.scorpio92.vkmd.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import ru.scorpio92.vkmd.Types.MainDB;
import ru.scorpio92.vkmd.View.AuthActivity;

/**
 * Created by scorpio92 on 10.12.16.
 */

public class VKMDUtils {

    public static String checkAutoEnter(Context context) {
        try {
            String select = "SELECT * FROM " + MainDB.LOGIN_TABLE + " WHERE " + MainDB.LOGIN_TABLE_AUTO_ENTER_COLUMN + "=" + "'1'";
            ArrayList<String> als = new ArrayList<String>();
            als.add(MainDB.LOGIN_TABLE_VALUE_COLUMN);
            ArrayList<String> result = DBUtils.select_from_db(context, select, als, false);
            if(!result.isEmpty()) {
                return result.get(0);
            }
        } catch (Exception e){e.printStackTrace();}
        return null;
    }

    public static boolean checkMediaLibrary(Context context) {

        ArrayList<String> result, als;
        try {
            String selectAllTracks = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_WAS_DOWNLOADED_COLUMN + " = " + "'1'" + " ORDER BY " + MainDB.MEDIA_TABLE_TRACK_ID_COLUMN + " LIMIT 1";
            als = new ArrayList<>();
            als.add(MainDB.MEDIA_TABLE_TRACK_ID_COLUMN);
            result = DBUtils.select_from_db(context, selectAllTracks, als, true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if(!result.isEmpty()) {
            return true;
        }

        Log.w("checkMediaLibrary", "no tracks in library");

        return false;
    }

    public static ArrayList<String> loadLastLogins(Context context) {
        ArrayList<String> result = new ArrayList<>();
        try {
            String selectAlllogins = "SELECT * FROM " + MainDB.LOGIN_TABLE;
            ArrayList<String> als = new ArrayList<String>();
            als.add(MainDB.LOGIN_TABLE_VALUE_COLUMN);
            result = DBUtils.select_from_db(context, selectAlllogins, als, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void writeCurrentLogin(Context context, String USER_ID, boolean autoEnter, int authMethod) {
        try {
            String checkInputedLogin = "SELECT * FROM " + MainDB.LOGIN_TABLE + " WHERE " + MainDB.LOGIN_TABLE_VALUE_COLUMN + "=" + "'" + USER_ID + "'";
            ArrayList<String> als = new ArrayList<String>();
            als.add(MainDB.LOGIN_TABLE_VALUE_COLUMN);
            als.add(MainDB.LOGIN_TABLE_AUTO_ENTER_COLUMN);
            if (DBUtils.select_from_db(context, checkInputedLogin, als, true).isEmpty()) {
                ContentValues newValues = new ContentValues();
                newValues.put(MainDB.LOGIN_TABLE_VALUE_COLUMN, USER_ID);
                newValues.put(MainDB.LOGIN_TABLE_AUTO_ENTER_COLUMN, "0");
                DBUtils.insert_update_delete(context, MainDB.LOGIN_TABLE, newValues, null, DBUtils.ACTION_INSERT);
                newValues.clear();
            }

            updateAutoEnterInfo(context, "0", null);
            if(autoEnter && (authMethod == AuthActivity.GET_TRACK_LIST_METHOD_BY_UID || authMethod == AuthActivity.GET_TRACK_LIST_METHOD_BY_GID))
                updateAutoEnterInfo(context, "1", USER_ID);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean updateAutoEnterInfo(Context context, String value, String login) {
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(MainDB.LOGIN_TABLE_AUTO_ENTER_COLUMN, value);
            String where;
            if(login == null) {
                where = MainDB.LOGIN_TABLE_VALUE_COLUMN + "!=" + "'" + "null" + "'";
            } else {
                where = MainDB.LOGIN_TABLE_VALUE_COLUMN + "=" + "'" + login + "'";
            }

            DBUtils.insert_update_delete(context, MainDB.LOGIN_TABLE, newValues, where, DBUtils.ACTION_UPDATE);
            return true;
        } catch (Exception e) {e.printStackTrace();}
        return false;
    }

    public static ArrayList<File> getSavedMP3FilesList(String path) {
        ArrayList<File> filesResult = new ArrayList<>();
        try {
            File[] files = new File(path).listFiles();
            for (File f : files) {
                if(f.isFile() && f.getName().contains(".mp3"))
                    filesResult.add(f);
            }
        } catch (Exception e) { e.printStackTrace();}
        return filesResult;
    }
}
