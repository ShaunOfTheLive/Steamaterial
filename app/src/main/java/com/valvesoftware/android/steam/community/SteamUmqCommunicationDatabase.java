package com.valvesoftware.android.steam.community;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.Message;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UmqInfo;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UserConversationInfo;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

public class SteamUmqCommunicationDatabase implements ISteamUmqCommunicationDatabase {
    private static final BigInteger BIGINT_STEAMID32BIT;
    private static final BigInteger BIGINT_STEAMID_HIGHBIT;
    private static final String DB_C_MSG_BINDATA = "bindata";
    private static final String DB_C_MSG_ID = "_id";
    private static final String DB_C_MSG_INCOMING = "msgincoming";
    private static final String DB_C_MSG_MYUSER1 = "myuser1";
    private static final String DB_C_MSG_MYUSER2 = "myuser2";
    private static final String DB_C_MSG_TIME = "msgtime";
    private static final String DB_C_MSG_TYPE = "msgtype";
    private static final String DB_C_MSG_UNREAD = "msgunread";
    private static final String DB_C_MSG_WUSER1 = "wuser1";
    private static final String DB_C_MSG_WUSER2 = "wuser2";
    private static final String DB_C_USERINFO_ID1 = "id1";
    private static final String DB_C_USERINFO_ID2 = "id2";
    private static final String DB_C_USERINFO_NAME = "name";
    private static final String DB_IDX_MSG_MYUSER = "idxMyUser";
    private static final String DB_IDX_MSG_TIME = "idxTime";
    private static final String DB_IDX_MSG_UNREAD = "idxUnread";
    private static final String DB_IDX_MSG_WUSER = "idxWUser";
    private static final String DB_IDX_USERINFO_ID = "idxUserInfoId";
    private static final String DB_NAME = "umqcomm.db";
    private static final String DB_T_MSG = "UmqMsg";
    private static final String DB_T_USERINFO = "UmqInfo";
    private static final int DB_VERSION = 2;
    private static final String SQL_SELECT_INFO_TEMPLATE = "SELECT id1, id2, name FROM UmqInfo WHERE ";
    private static final String SQL_SELECT_MSG_TEMPLATE = "SELECT _id, myuser1,myuser2,wuser1,wuser2,msgincoming,msgunread,msgtime,msgtype,bindata FROM UmqMsg WHERE ";
    private SQLiteStatement dbStmt_SQL_DELETE_MSG_WITH_ALL;
    private SQLiteStatement dbStmt_SQL_DELETE_MSG_WITH_USER;
    private SQLiteStatement dbStmt_SQL_INSERT_INFO;
    private SQLiteStatement dbStmt_SQL_INSERT_MSG;
    private SQLiteStatement dbStmt_SQL_SELECT_MSG_UNREAD_COUNT;
    private SQLiteStatement dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER;
    private SQLiteStatement dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_ALL;
    private SQLiteStatement dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_USER;
    private Context m_context;
    private SQLiteDatabase m_db;
    private DbOpenHelper m_helper;

    private class DbOpenHelper extends SQLiteOpenHelper {
        public DbOpenHelper(Context context) {
            super(context, DB_NAME, null, 2);
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE UmqMsg (   _id integer primary key autoincrement , myuser1 integer not null , myuser2 integer not null , wuser1 integer not null , wuser2 integer not null , msgincoming integer not null , msgunread integer not null , msgtime integer not null , msgtype text not null , bindata text not null  )");
            database.execSQL("CREATE INDEX idxMyUser ON UmqMsg ( myuser1,myuser2 )");
            database.execSQL("CREATE INDEX idxWUser ON UmqMsg ( wuser1,wuser2 )");
            database.execSQL("CREATE INDEX idxUnread ON UmqMsg ( msgunread )");
            database.execSQL("CREATE INDEX idxTime ON UmqMsg ( msgtime )");
            database.execSQL("CREATE TABLE UmqInfo (   id1 integer not null , id2 integer not null , name text not null  )");
            database.execSQL("CREATE UNIQUE INDEX idxUserInfoId ON UmqInfo ( id1,id2 )");
        }

        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            database.execSQL("DROP TABLE if exists UmqMsg");
            onCreate(database);
        }
    }

    public SteamUmqCommunicationDatabase(Context context) {
        this.dbStmt_SQL_INSERT_MSG = null;
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT = null;
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER = null;
        this.dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_USER = null;
        this.dbStmt_SQL_DELETE_MSG_WITH_USER = null;
        this.dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_ALL = null;
        this.dbStmt_SQL_DELETE_MSG_WITH_ALL = null;
        this.dbStmt_SQL_INSERT_INFO = null;
        this.m_context = context;
        this.m_helper = new DbOpenHelper(this.m_context);
        this.m_db = this.m_helper.getWritableDatabase();
    }

    static {
        BIGINT_STEAMID32BIT = BigInteger.valueOf(4294967296L);
        BIGINT_STEAMID_HIGHBIT = BigInteger.valueOf(2147483648L);
    }

    private static int[] steamidSplit(String sSteamId) {
        BigInteger[] xsplit = new BigInteger(sSteamId).divideAndRemainder(BIGINT_STEAMID32BIT);
        BigInteger[] x0 = xsplit[0].divideAndRemainder(BIGINT_STEAMID_HIGHBIT);
        BigInteger[] x1 = xsplit[1].divideAndRemainder(BIGINT_STEAMID_HIGHBIT);
        int i0 = x0[0].equals(BigInteger.ZERO) ? x0[1].intValue() : x0[1].intValue() != 0 ? -x0[1].intValue() : Integer.MIN_VALUE;
        int i1 = x1[0].equals(BigInteger.ZERO) ? x1[1].intValue() : x1[1].intValue() != 0 ? -x1[1].intValue() : Integer.MIN_VALUE;
        return new int[]{i0, i1};
    }

    private static String steamidJoin(int i0, int i1) {
        BigInteger x0;
        BigInteger x1;
        if (i0 == Integer.MIN_VALUE) {
            x0 = BIGINT_STEAMID_HIGHBIT;
        } else {
            x0 = i0 >= 0 ? BigInteger.valueOf((long) i0) : BigInteger.valueOf((long) (-i0)).or(BIGINT_STEAMID_HIGHBIT);
        }
        if (i1 == Integer.MIN_VALUE) {
            x1 = BIGINT_STEAMID_HIGHBIT;
        } else {
            x1 = i1 >= 0 ? BigInteger.valueOf((long) i1) : BigInteger.valueOf((long) (-i1)).or(BIGINT_STEAMID_HIGHBIT);
        }
        return x0.multiply(BIGINT_STEAMID32BIT).add(x1).toString();
    }

    private static Calendar dateFromDbTimestamp(int dbTimestamp) {
        Calendar cTS = Calendar.getInstance();
        cTS.setTimeInMillis(((long) dbTimestamp) * 1000);
        return cTS;
    }

    public synchronized boolean insertMessage(Message in) {
        long j = 1;
        boolean z = true;
        synchronized (this) {
            long j2;
            if (this.dbStmt_SQL_INSERT_MSG == null) {
                this.dbStmt_SQL_INSERT_MSG = this.m_db.compileStatement("INSERT INTO UmqMsg ( myuser1,myuser2,wuser1,wuser2,msgincoming,msgunread,msgtime,msgtype,bindata ) VALUES ( ?,?, ?,?, ?,?,?, ?,? )");
            }
            int[] myuser = steamidSplit(in.sMySteamID);
            this.dbStmt_SQL_INSERT_MSG.bindLong(1, (long) myuser[0]);
            this.dbStmt_SQL_INSERT_MSG.bindLong(DB_VERSION, (long) myuser[1]);
            int[] wuser = steamidSplit(in.sWithSteamID);
            this.dbStmt_SQL_INSERT_MSG.bindLong(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER, (long) wuser[0]);
            this.dbStmt_SQL_INSERT_MSG.bindLong(AccessibilityNodeInfoCompat.ACTION_SELECT, (long) wuser[1]);
            SQLiteStatement sQLiteStatement = this.dbStmt_SQL_INSERT_MSG;
            if (in.bIncoming) {
                j2 = 1;
            } else {
                j2 = 0;
            }
            sQLiteStatement.bindLong(MotionEventCompat.ACTION_POINTER_DOWN, j2);
            SQLiteStatement sQLiteStatement2 = this.dbStmt_SQL_INSERT_MSG;
            if (!in.bUnread) {
                j = 0;
            }
            sQLiteStatement2.bindLong(MotionEventCompat.ACTION_POINTER_UP, j);
            this.dbStmt_SQL_INSERT_MSG.bindLong(MotionEventCompat.ACTION_HOVER_MOVE, (long) ((int) (in.msgtime.getTimeInMillis() / 1000)));
            this.dbStmt_SQL_INSERT_MSG.bindString(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION, in.msgtype);
            this.dbStmt_SQL_INSERT_MSG.bindString(9, in.bindata);
            in.id = (int) this.dbStmt_SQL_INSERT_MSG.executeInsert();
            if (in.id == -1) {
                z = false;
            }
        }
        return z;
    }

    private Message selectProcessSingleMessage(Cursor row) {
        boolean z = true;
        Message msg = new Message();
        msg.id = row.getInt(0);
        msg.sMySteamID = steamidJoin(row.getInt(1), row.getInt(DB_VERSION));
        msg.sWithSteamID = steamidJoin(row.getInt(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER), row.getInt(AccessibilityNodeInfoCompat.ACTION_SELECT));
        msg.bIncoming = row.getInt(MotionEventCompat.ACTION_POINTER_DOWN) != 0;
        if (row.getInt(MotionEventCompat.ACTION_POINTER_UP) == 0) {
            z = false;
        }
        msg.bUnread = z;
        msg.msgtime = dateFromDbTimestamp(row.getInt(MotionEventCompat.ACTION_HOVER_MOVE));
        msg.msgtype = row.getString(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        msg.bindata = row.getString(9);
        return msg;
    }

    private ArrayList<Message> selectProcessCursor(Cursor rows) {
        ArrayList<Message> result = new ArrayList();
        rows.moveToFirst();
        while (!rows.isAfterLast()) {
            result.add(selectProcessSingleMessage(rows));
            rows.moveToNext();
        }
        rows.close();
        return result;
    }

    private ArrayList<Message> selectMessagesWhere(String where) {
        return selectProcessCursor(this.m_db.rawQuery(SQL_SELECT_MSG_TEMPLATE + where, null));
    }

    public synchronized ArrayList<Message> selectAllMessages() {
        return selectMessagesWhere("1");
    }

    public synchronized ArrayList<Message> selectMessagesWithUser(String sMySteamID, String sWithSteamID, int nCount, Message lastKnown) {
        int[] myuser;
        int[] wuser;
        myuser = steamidSplit(sMySteamID);
        wuser = steamidSplit(sWithSteamID);
        return selectMessagesWhere("myuser1=" + myuser[0] + " AND " + DB_C_MSG_MYUSER2 + "=" + myuser[1] + " AND " + DB_C_MSG_WUSER1 + "=" + wuser[0] + " AND " + DB_C_MSG_WUSER2 + "=" + wuser[1] + (lastKnown != null ? " AND _id<" + lastKnown.id : "") + " ORDER BY " + DB_C_MSG_ID + " DESC " + " LIMIT " + nCount);
    }

    public ArrayList<Message> selectMessagesWithUserLatest(String sMySteamID, String sWithSteamID, int msgidFirst) {
        int[] myuser = steamidSplit(sMySteamID);
        int[] wuser = steamidSplit(sWithSteamID);
        return selectMessagesWhere("myuser1=" + myuser[0] + " AND " + DB_C_MSG_MYUSER2 + "=" + myuser[1] + " AND " + DB_C_MSG_WUSER1 + "=" + wuser[0] + " AND " + DB_C_MSG_WUSER2 + "=" + wuser[1] + " AND " + DB_C_MSG_ID + ">=" + msgidFirst + " ORDER BY " + DB_C_MSG_ID + " DESC ");
    }

    public synchronized ArrayList<Message> selectMessagesByID(int id) {
        return selectMessagesWhere("_id=" + id);
    }

    public synchronized int selectCountOfUnreadMessages(String sMySteamID) {
        if (this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT == null) {
            this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT = this.m_db.compileStatement("SELECT COUNT(*) FROM UmqMsg WHERE msgunread=1  AND myuser1=?  AND myuser2=? ");
        }
        int[] myuser = steamidSplit(sMySteamID);
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT.bindLong(1, (long) myuser[0]);
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT.bindLong(DB_VERSION, (long) myuser[1]);
        return (int) this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT.simpleQueryForLong();
    }

    public synchronized int selectCountOfUnreadMessagesWithUser(String sMySteamID, String sWithSteamID) {
        if (this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER == null) {
            this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER = this.m_db.compileStatement("SELECT COUNT(*) FROM UmqMsg WHERE msgunread=1  AND myuser1=?  AND myuser2=?  AND wuser1=?  AND wuser2=? ");
        }
        int[] myuser = steamidSplit(sMySteamID);
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER.bindLong(1, (long) myuser[0]);
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER.bindLong(DB_VERSION, (long) myuser[1]);
        int[] wuser = steamidSplit(sWithSteamID);
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER.bindLong(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER, (long) wuser[0]);
        this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER.bindLong(AccessibilityNodeInfoCompat.ACTION_SELECT, (long) wuser[1]);
        return (int) this.dbStmt_SQL_SELECT_MSG_UNREAD_COUNT_WITH_USER.simpleQueryForLong();
    }

    public synchronized UserConversationInfo selectUserConversationInfo(String sMySteamID, String sWithSteamID) {
        UserConversationInfo result;
        int[] myuser = steamidSplit(sMySteamID);
        int[] wuser = steamidSplit(sWithSteamID);
        Cursor rows = this.m_db.rawQuery(" SELECT SUM( msgunread),  COUNT( * ), msgtime, _id FROM UmqMsg WHERE myuser1=" + myuser[0] + " AND " + DB_C_MSG_MYUSER2 + "=" + myuser[1] + " AND " + DB_C_MSG_WUSER1 + "=" + wuser[0] + " AND " + DB_C_MSG_WUSER2 + "=" + wuser[1] + " ORDER BY " + DB_C_MSG_ID + " DESC " + " LIMIT 1 ", null);
        result = new UserConversationInfo();
        if (rows.moveToFirst() && !rows.isAfterLast()) {
            int count = rows.getInt(1);
            if (count > 0) {
                result.numMsgsTotal = count;
                result.numUnreadMsgs = rows.getInt(0);
                result.latestTimestamp = dateFromDbTimestamp(rows.getInt(DB_VERSION));
                result.latestMsgId = rows.getInt(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER);
            }
            rows.close();
        }
        return result;
    }

    public synchronized void updateMarkReadMessagesWithUser(String sMySteamID, String sWithSteamID, boolean deleteAll) {
        SQLiteStatement stmt;
        SQLiteDatabase sQLiteDatabase;
        StringBuilder append;
        String str;
        if (deleteAll) {
            stmt = sWithSteamID != null ? this.dbStmt_SQL_DELETE_MSG_WITH_USER : this.dbStmt_SQL_DELETE_MSG_WITH_ALL;
            if (stmt == null) {
                sQLiteDatabase = this.m_db;
                append = new StringBuilder().append("DELETE FROM UmqMsg WHERE myuser1=?  AND myuser2=? ");
                if (sWithSteamID != null) {
                    str = " AND wuser1=?  AND wuser2=? ";
                } else {
                    str = "";
                }
                stmt = sQLiteDatabase.compileStatement(append.append(str).toString());
                if (sWithSteamID != null) {
                    this.dbStmt_SQL_DELETE_MSG_WITH_USER = stmt;
                } else {
                    this.dbStmt_SQL_DELETE_MSG_WITH_ALL = stmt;
                }
            }
        } else {
            stmt = sWithSteamID != null ? this.dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_USER : this.dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_ALL;
            if (stmt == null) {
                sQLiteDatabase = this.m_db;
                append = new StringBuilder().append("UPDATE UmqMsg SET msgunread=0  WHERE msgunread=1  AND myuser1=?  AND myuser2=? ");
                if (sWithSteamID != null) {
                    str = " AND wuser1=?  AND wuser2=? ";
                } else {
                    str = "";
                }
                stmt = sQLiteDatabase.compileStatement(append.append(str).toString());
                if (sWithSteamID != null) {
                    this.dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_USER = stmt;
                } else {
                    this.dbStmt_SQL_UPDATE_MSG_UNREAD_WITH_ALL = stmt;
                }
            }
        }
        int[] myuser = steamidSplit(sMySteamID);
        stmt.bindLong(1, (long) myuser[0]);
        stmt.bindLong(DB_VERSION, (long) myuser[1]);
        if (sWithSteamID != null) {
            int[] wuser = steamidSplit(sWithSteamID);
            stmt.bindLong(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER, (long) wuser[0]);
            stmt.bindLong(AccessibilityNodeInfoCompat.ACTION_SELECT, (long) wuser[1]);
        }
        stmt.execute();
    }

    public synchronized boolean insertInfo(UmqInfo in) {
        if (this.dbStmt_SQL_INSERT_INFO == null) {
            this.dbStmt_SQL_INSERT_INFO = this.m_db.compileStatement("INSERT OR REPLACE INTO UmqInfo ( id1,id2,name ) VALUES ( ?,?, ? )");
        }
        int[] steamid = steamidSplit(in.steamid);
        this.dbStmt_SQL_INSERT_INFO.bindLong(1, (long) steamid[0]);
        this.dbStmt_SQL_INSERT_INFO.bindLong(DB_VERSION, (long) steamid[1]);
        this.dbStmt_SQL_INSERT_INFO.bindString(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER, in.name);
        this.dbStmt_SQL_INSERT_INFO.executeInsert();
        return true;
    }

    private UmqInfo selectProcessSingleInfo(Cursor row) {
        UmqInfo msg = new UmqInfo();
        msg.steamid = steamidJoin(row.getInt(0), row.getInt(1));
        msg.name = row.getString(DB_VERSION);
        return msg;
    }

    private ArrayList<UmqInfo> selectProcessInfo(Cursor rows) {
        ArrayList<UmqInfo> result = new ArrayList();
        rows.moveToFirst();
        while (!rows.isAfterLast()) {
            result.add(selectProcessSingleInfo(rows));
            rows.moveToNext();
        }
        rows.close();
        return result;
    }

    private ArrayList<UmqInfo> selectInfoWhere(String where) {
        return selectProcessInfo(this.m_db.rawQuery(SQL_SELECT_INFO_TEMPLATE + where, null));
    }

    public synchronized UmqInfo selectInfo(String steamid) {
        UmqInfo umqInfo;
        int[] sid = steamidSplit(steamid);
        ArrayList<UmqInfo> list = selectInfoWhere("id1=" + sid[0] + " AND " + DB_C_USERINFO_ID2 + "=" + sid[1] + " LIMIT 1");
        if (list == null || list.isEmpty()) {
            umqInfo = null;
        } else {
            umqInfo = (UmqInfo) list.get(0);
        }
        return umqInfo;
    }
}
