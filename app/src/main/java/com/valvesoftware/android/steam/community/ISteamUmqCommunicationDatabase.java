package com.valvesoftware.android.steam.community;

import java.util.ArrayList;
import java.util.Calendar;

public interface ISteamUmqCommunicationDatabase {

    public static class Message {
        public boolean bIncoming;
        public boolean bUnread;
        public String bindata;
        public int id;
        public Calendar msgtime;
        public String msgtype;
        public String sMySteamID;
        public String sWithSteamID;

        public String toString() {
            return "Message:[ ID=" + this.id + " MYSTEAMID=" + this.sMySteamID + " WSTEAMID=" + this.sWithSteamID + " incoming=" + this.bIncoming + " unread=" + this.bUnread + " time=" + this.msgtime.getTime().toLocaleString() + " type=" + this.msgtype + " data=//" + this.bindata + "// ]";
        }
    }

    public static class UmqInfo {
        public String name;
        public String steamid;
    }

    public static class UserConversationInfo {
        public int latestMsgId;
        public Calendar latestTimestamp;
        public int numMsgsTotal;
        public int numUnreadMsgs;
    }

    ArrayList<Message> selectAllMessages();

    int selectCountOfUnreadMessages(String str);

    int selectCountOfUnreadMessagesWithUser(String str, String str2);

    UmqInfo selectInfo(String str);

    ArrayList<Message> selectMessagesByID(int i);

    ArrayList<Message> selectMessagesWithUser(String str, String str2, int i, Message message);

    ArrayList<Message> selectMessagesWithUserLatest(String str, String str2, int i);

    UserConversationInfo selectUserConversationInfo(String str, String str2);

    void updateMarkReadMessagesWithUser(String str, String str2, boolean z);
}
