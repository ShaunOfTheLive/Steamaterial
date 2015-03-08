package com.valvesoftware.android.steam.community;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamDebugUtil.DebugUtilRecord;
import java.util.ArrayList;

public class SteamDebugUtilDatabase {
    private static final String DB_C_DBG_ID = "_id";
    private static final String DB_C_DBG_KEY = "key";
    private static final String DB_C_DBG_PARENT_ID = "parent_id";
    private static final String DB_C_DBG_SESSIONSTATE = "sessionstate";
    private static final String DB_C_DBG_THREAD_ID = "threadid";
    private static final String DB_C_DBG_TIME = "msgtime";
    private static final String DB_C_DBG_VALUE = "value";
    private static final String DB_IDX_DBG_SESSIONSTATE = "idxDbgSessionState";
    private static final String DB_NAME = "dbgutil.db";
    private static final String DB_T_DBG = "dbgutil";
    private static final long DB_VAL_DBG_SESSIONSTATE_EXCEPTION = 2;
    private static final long DB_VAL_DBG_SESSIONSTATE_TRACE = 1;
    private static final long DB_VAL_DBG_SESSIONSTATE_TRACE_UPLOADED = 3;
    private static final int DB_VERSION = 1;
    private SQLiteStatement dbStmt_SQL_INSERT;
    private SQLiteStatement dbStmt_SQL_UPDATE;
    private Context m_context;
    private SQLiteDatabase m_db;
    private DbOpenHelper m_helper;

    public static class CrashTrace {
        public int id;
        public String info;
        public long timestamp;
    }

    private class DbOpenHelper extends SQLiteOpenHelper {
        public DbOpenHelper(Context context) {
            super(context, DB_NAME, null, 1);
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE dbgutil (   _id integer primary key autoincrement , parent_id integer default null , threadid integer not null , msgtime integer not null , key text default null , value text default null , sessionstate integer default null  )");
            database.execSQL("CREATE INDEX idxDbgSessionState ON dbgutil ( sessionstate )");
        }

        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            database.execSQL("DROP TABLE if exists dbgutil");
            onCreate(database);
        }
    }

    public SteamDebugUtilDatabase(Context context) {
        this.dbStmt_SQL_INSERT = null;
        this.dbStmt_SQL_UPDATE = null;
        this.m_context = context;
        this.m_helper = new DbOpenHelper(this.m_context);
        this.m_db = this.m_helper.getWritableDatabase();
    }

    public String getDbName() {
        return DB_NAME;
    }

    public synchronized boolean insertMessage(DebugUtilRecord in) {
        boolean z = true;
        synchronized (this) {
            if (this.dbStmt_SQL_INSERT == null) {
                this.dbStmt_SQL_INSERT = this.m_db.compileStatement("INSERT INTO dbgutil ( parent_id,threadid,msgtime,key,value,sessionstate ) VALUES ( ?,?, ?, ?,?, ? )");
            }
            if (in.parent != null) {
                this.dbStmt_SQL_INSERT.bindLong(DB_VERSION, in.parent.getId());
            } else {
                this.dbStmt_SQL_INSERT.bindNull(DB_VERSION);
            }
            this.dbStmt_SQL_INSERT.bindLong(UriData.RESULT_HTTP_EXCEPTION, in.threadid);
            this.dbStmt_SQL_INSERT.bindLong(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER, in.timestamp.getTimeInMillis() / 1000);
            this.dbStmt_SQL_INSERT.bindString(AccessibilityNodeInfoCompat.ACTION_SELECT, in.key != null ? in.key : "");
            this.dbStmt_SQL_INSERT.bindString(MotionEventCompat.ACTION_POINTER_DOWN, in.value != null ? in.value : "");
            if (in.sessionstate > 0) {
                this.dbStmt_SQL_INSERT.bindLong(MotionEventCompat.ACTION_POINTER_UP, in.sessionstate);
            } else if (in.key == null) {
                this.dbStmt_SQL_INSERT.bindLong(MotionEventCompat.ACTION_POINTER_UP, in.value == null ? DB_VAL_DBG_SESSIONSTATE_EXCEPTION : DB_VAL_DBG_SESSIONSTATE_TRACE);
            } else {
                this.dbStmt_SQL_INSERT.bindNull(MotionEventCompat.ACTION_POINTER_UP);
            }
            in.id = this.dbStmt_SQL_INSERT.executeInsert();
            if (in.id == -1) {
                z = false;
            }
        }
        return z;
    }

    public synchronized ArrayList<CrashTrace> selectCrashTraces() {
        ArrayList<CrashTrace> result;
        result = new ArrayList();
        Cursor rows = this.m_db.rawQuery("SELECT _id,msgtime,value FROM dbgutil WHERE sessionstate=1", null);
        rows.moveToFirst();
        while (!rows.isAfterLast()) {
            CrashTrace x = new CrashTrace();
            x.id = rows.getInt(0);
            x.timestamp = rows.getLong(DB_VERSION);
            x.info = rows.getString(UriData.RESULT_HTTP_EXCEPTION);
            result.add(x);
            rows.moveToNext();
        }
        rows.close();
        return result;
    }

    public synchronized void updateMarkCrashTraceUploaded(CrashTrace x) {
        if (this.dbStmt_SQL_UPDATE == null) {
            this.dbStmt_SQL_UPDATE = this.m_db.compileStatement("DELETE FROM dbgutil WHERE _id<=?");
        }
        this.dbStmt_SQL_UPDATE.bindLong(DB_VERSION, (long) x.id);
        this.dbStmt_SQL_UPDATE.execute();
    }
}
