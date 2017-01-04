package ru.scorpio92.vkmd.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import ru.scorpio92.vkmd.Types.MainDB;

/**
 * Created by scorpio92 on 28.03.16.
 */
public class DBUtils {

    public static final int ACTION_INSERT=1;
    public static final int ACTION_DELETE=2;
    public static final int ACTION_UPDATE=3;

    //select из БД
    public static ArrayList<String> select_from_db(Context context, String query, ArrayList<String> columns, Boolean selectAll) {
        ArrayList<String> s=new ArrayList<String>();
        SQLiteOpenHelper mDB=null;
        SQLiteDatabase sdb=null;
        Cursor cursor=null;
        try {
            mDB = new MainDB(context);
            sdb = mDB.getWritableDatabase();

            cursor = sdb.rawQuery(query,null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    if(selectAll) { //если нужно выбрать все записи
                        if (cursor.moveToFirst()) {
                            do {
                                for(int i=0;i<columns.size();i++) {
                                    s.add(cursor.getString(cursor.getColumnIndex(columns.get(i))));
                                }
                            } while (cursor.moveToNext());
                        }
                    }
                    else {
                        cursor.moveToFirst();
                        for (int i = 0; i < columns.size(); i++) {
                            s.add(cursor.getString(cursor.getColumnIndex(columns.get(i))));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Log.e("select_from_db", null, e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (sdb != null) {
                sdb.close();
            }
            if (mDB != null) {
                mDB.close();
            }
        }
        return s;
    }

    //обработка запросов insert, delete, update
    public static void insert_update_delete (Context context, String table, ContentValues newValues, String where, int action) {
        SQLiteDatabase sdb = null;
        SQLiteOpenHelper mDB=null;

        try {
            mDB = new MainDB(context);
            sdb = mDB.getWritableDatabase();

            switch (action) {
                case ACTION_INSERT:
                    sdb.insert(table, null, newValues);
                    break;
                case ACTION_UPDATE:
                    sdb.update(table, newValues, where,null);
                    break;
                case ACTION_DELETE:
                    sdb.delete(table, where, null);
                    break;
            }
        }
        catch (Exception e){
            Log.w("insert_update_delete ", e.toString());
        }
        finally {
            if (sdb != null) {
                sdb.close();
            }
            if (mDB != null) {
                mDB.close();
            }
        }
    }
}
