package com.valvesoftware.android.steam.community;

import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.valvesoftware.android.steam.community.FriendInfoDB.ChatsUpdateType;
import com.valvesoftware.android.steam.community.ISteamDebugUtil.IDebugUtilRecord;
import com.valvesoftware.android.steam.community.SteamDBDiskCache.IndefiniteCache;
import com.valvesoftware.android.steam.community.SteamDBDiskCache.WebCache;
import com.valvesoftware.android.steam.community.SteamDebugUtil.Dummy_ISteamDebugUtil;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamUriHandler.CommandProperty;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.activity.ChatActivity;
import com.valvesoftware.android.steam.community.activity.CommunityActivity;
import com.valvesoftware.android.steam.community.activity.CommunityGroupsActivity;
import com.valvesoftware.android.steam.community.activity.LoginActivity;
import com.valvesoftware.android.steam.community.activity.SettingsActivity;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Calendar;

public class SteamCommunityApplication extends Application {
    public static final String INTENT_ACTION_WEBVIEW_RESULT = "com.valvesoftware.android.steam.community.intent.action.WEBVIEW_RESULT";
    public static CrashHandler m_CrashHandler;
    private static SteamCommunityApplication m_singleton;
    public boolean m_bApplicationExiting;
    private boolean m_bOverTheAirVersion;
    private IndefiniteCache m_diskCacheIndefinite;
    private WebCache m_diskCacheWeb;
    private FriendInfoDB m_friendInfoDB;
    private GroupInfoDB m_groupInfoDB;
    private SettingInfoDB m_settingInfoDB;
    private SteamDBServiceConnection m_steamDBConnection;
    private final UmqDBCallback m_umqdbIntentReceiver;

    public static class CrashHandler implements UncaughtExceptionHandler {
        public ISteamDebugUtil m_dbgUtil;
        private UncaughtExceptionHandler m_defaultUEH;

        public CrashHandler() {
            this.m_dbgUtil = null;
            this.m_defaultUEH = null;
        }

        public void register() {
            com.valvesoftware.android.steam.community.SteamCommunityApplication.CrashHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            if (defaultUEH != this) {
                this.m_defaultUEH = defaultUEH;
                Thread.setDefaultUncaughtExceptionHandler(this);
            }
        }

        public void uncaughtException(Thread thread, Throwable ex) {
            Writer exWriter = new StringWriter();
            ex.printStackTrace(new PrintWriter(exWriter));
            Calendar cal = Calendar.getInstance();
            IDebugUtilRecord r = GetSteamDebugUtil().newDebugUtilRecord(null, null, "VERSION: " + Config.APP_VERSION_ID + "\n" + "APPNAME: com.valvesoftware.android.steam.community\n" + "APPVERSION: " + Config.APP_VERSION + "\n" + "TIMESTAMP: " + (cal.getTimeInMillis() / 1000) + "\n" + "DATETIME: " + cal.getTime().toGMTString() + "\n" + "USERID: " + VERSION.CODENAME + VERSION.INCREMENTAL + " (" + Build.DEVICE + "/" + Build.PRODUCT + ") " + Build.BRAND + " - " + Build.MANUFACTURER + " - " + Build.DISPLAY + "\n" + "CONTACT: " + SteamWebApi.GetLoginSteamID() + "\n" + "SYSTEMVER: " + VERSION.RELEASE + " : " + VERSION.SDK + "\n" + "SYSTEMOS: " + Build.MODEL + "\n" + "STACKTRACE: " + "\n" + exWriter.toString() + "\n//ENDOFSTACKTRACE//");
            IDebugUtilRecord newDebugUtilRecord = GetSteamDebugUtil().newDebugUtilRecord(r, null, (String) null);
            newDebugUtilRecord = GetSteamDebugUtil().newDebugUtilRecord(r, null, (String) null);
            long lStartTime = System.currentTimeMillis();
            while (newDebugUtilRecord.getId() == 0 && System.currentTimeMillis() - lStartTime < 5000) {
                try {
                    Thread.sleep(450);
                } catch (InterruptedException e) {
                }
            }
            if (this.m_defaultUEH != null) {
                this.m_defaultUEH.uncaughtException(thread, ex);
            }
        }

        private ISteamDebugUtil GetSteamDebugUtil() {
            return this.m_dbgUtil != null ? this.m_dbgUtil : SteamCommunityApplication.GetSteamDebugUtil();
        }
    }

    private class UmqDBCallback extends BroadcastReceiver {
        private UmqDBCallback() {
        }

        public void onReceive(Context context, Intent intent) {
            String notificationType = intent.getStringExtra("type");
            if (notificationType.equalsIgnoreCase("chatmsg")) {
                String action = intent.getStringExtra("action");
                int msgid;
                if ("incoming".equals(action)) {
                    msgid = intent.getIntExtra("msgidLast", -1);
                    if (msgid > 0) {
                        SteamCommunityApplication.this.GetFriendInfoDB().ChatsWithUserUpdate(intent.getStringExtra("steamid"), msgid, ChatsUpdateType.msg_incoming, intent.getIntExtra("incoming", 0) + intent.getIntExtra("my_incoming", 0));
                    }
                } else if ("read".equals(action)) {
                    SteamCommunityApplication.this.GetFriendInfoDB().ChatsWithUserUpdate(intent.getStringExtra("steamid"), -1, ChatsUpdateType.mark_read, 0);
                } else if ("clear".equals(action)) {
                    SteamCommunityApplication.this.GetFriendInfoDB().ChatsWithUserUpdate(intent.getStringExtra("steamid"), -1, ChatsUpdateType.clear, 0);
                } else if ("send".equals(action)) {
                    msgid = intent.getIntExtra("msgid", -1);
                    if (msgid > 0) {
                        SteamCommunityApplication.this.GetFriendInfoDB().ChatsWithUserUpdate(intent.getStringExtra("steamid"), msgid, ChatsUpdateType.msg_sent, 1);
                    }
                }
            } else if (notificationType.equalsIgnoreCase("personastate")) {
                SteamCommunityApplication.this.GetFriendInfoDB().onReceive(context, intent);
            } else if (notificationType.equalsIgnoreCase("personarelationship")) {
                SteamCommunityApplication.this.GetFriendInfoDB().onReceive(context, intent);
                SteamCommunityApplication.this.GetGroupInfoDB().onReceive(context, intent);
            } else if (notificationType.equalsIgnoreCase("umqstate")) {
                SteamCommunityApplication.this.GetFriendInfoDB().onReceive(context, intent);
                SteamCommunityApplication.this.GetGroupInfoDB().onReceive(context, intent);
            }
        }
    }

    public static class UriNotification {
        public static final String NOTIFICATION_TAG = "URINOTIFICATION";
        public int actionDrawable;
        public PendingIntent actionPendingIntent;
        public String actionString;
        public int id;
        public String text;
        public String title;

        public boolean setActionInfo(String action) {
            if (action == null || action.equals("")) {
                return false;
            }
            this.actionString = action;
            String[] args = action.split(":");
            if (args.length < 1) {
                return false;
            }
            if (args[0].equals("CF")) {
                this.actionDrawable = 2130837528;
                this.actionPendingIntent = SteamCommunityApplication.GetInstance().GetIntentForActivityClass(CommunityActivity.class);
                return true;
            } else if (args[0].equals("CG")) {
                this.actionDrawable = 2130837529;
                this.actionPendingIntent = SteamCommunityApplication.GetInstance().GetIntentForActivityClass(CommunityGroupsActivity.class);
                return true;
            } else if (args[0].equals("SS")) {
                this.actionDrawable = 2130837531;
                this.actionPendingIntent = SteamCommunityApplication.GetInstance().GetIntentForActivityClass(SettingsActivity.class);
                return true;
            } else if (args[0].equals("M") && args.length >= 2 && args[1].length() > 0) {
                this.actionDrawable = 2130837539;
                this.actionPendingIntent = SteamCommunityApplication.GetInstance().GetIntentToChatWithSteamID(args[1]);
                return true;
            } else if (args[0].length() <= 0 || !args[0].startsWith("U") || args.length < 3 || args[1].length() <= 0 || args[2].length() <= 0) {
                return false;
            } else {
                boolean bCategoryView;
                boolean bStoreWebsite;
                String toString;
                this.actionDrawable = 2130837532;
                if (args[1].equals("CF")) {
                    this.actionDrawable = 2130837528;
                } else if (args[1].equals("CG")) {
                    this.actionDrawable = 2130837529;
                } else if (args[1].equals("SS")) {
                    this.actionDrawable = 2130837531;
                } else if (args[1].equals("SC")) {
                    this.actionDrawable = 2130837532;
                } else if (args[1].equals("SW")) {
                    this.actionDrawable = 2130837533;
                } else if (args[1].equals("ST")) {
                    this.actionDrawable = 2130837525;
                } else if (args[1].equals("SF")) {
                    this.actionDrawable = 2130837530;
                } else if (args[1].equals("FF")) {
                    this.actionDrawable = 2130837527;
                } else if (args[1].equals("MM")) {
                    this.actionDrawable = 2130837539;
                }
                if (args[0].indexOf("C", 1) > 0) {
                    bCategoryView = true;
                } else {
                    bCategoryView = false;
                }
                if (args[0].indexOf("S", 1) > 0) {
                    bStoreWebsite = true;
                } else {
                    bStoreWebsite = false;
                }
                SteamCommunityApplication GetInstance = SteamCommunityApplication.GetInstance();
                StringBuilder append = new StringBuilder().append(SteamUriHandler.MOBILE_PROTOCOL);
                if (bCategoryView) {
                    toString = Command.opencategoryurl.toString();
                } else {
                    toString = Command.openurl.toString();
                }
                append = append.append(toString).append("?").append(CommandProperty.url.toString()).append("=");
                toString = bStoreWebsite ? bCategoryView ? Config.URL_STORE_BASE_INSECURE : Config.URL_STORE_BASE : bCategoryView ? Config.URL_COMMUNITY_BASE_INSECURE : Config.URL_COMMUNITY_BASE;
                this.actionPendingIntent = GetInstance.GetIntentForUriString(append.append(toString).append("/").append(args[2]).toString());
                return true;
            }
        }

        public void setDefaultActionInfo() {
            this.actionString = "";
            this.actionDrawable = 2130837544;
            this.actionPendingIntent = SteamCommunityApplication.GetInstance().GetIntentForActivityClass(CommunityActivity.class);
        }
    }

    public SteamCommunityApplication() {
        this.m_diskCacheWeb = null;
        this.m_diskCacheIndefinite = null;
        this.m_steamDBConnection = null;
        this.m_friendInfoDB = null;
        this.m_groupInfoDB = null;
        this.m_settingInfoDB = null;
        this.m_bOverTheAirVersion = false;
        this.m_bApplicationExiting = false;
        this.m_umqdbIntentReceiver = new UmqDBCallback();
    }

    static {
        m_singleton = null;
        m_CrashHandler = new CrashHandler();
    }

    public static SteamCommunityApplication GetInstance() {
        return m_singleton;
    }

    public boolean IsOverTheAirVersion() {
        return this.m_bOverTheAirVersion;
    }

    public static ISteamDebugUtil GetSteamDebugUtil() {
        SteamDBService svc = null;
        if (m_singleton != null) {
            svc = m_singleton.GetSteamDB();
        }
        return svc != null ? svc.getDebugUtil() : new Dummy_ISteamDebugUtil();
    }

    public WebCache GetDiskCacheWeb() {
        return this.m_diskCacheWeb;
    }

    public IndefiniteCache GetDiskCacheIndefinite() {
        return this.m_diskCacheIndefinite;
    }

    public FriendInfoDB GetFriendInfoDB() {
        return this.m_friendInfoDB;
    }

    public GroupInfoDB GetGroupInfoDB() {
        return this.m_groupInfoDB;
    }

    public SettingInfoDB GetSettingInfoDB() {
        return this.m_settingInfoDB;
    }

    public void SubmitSteamDBRequest(RequestBase request) {
        this.m_steamDBConnection.SubmitRequest(request);
    }

    public SteamDBService GetSteamDB() {
        return this.m_steamDBConnection.GetService();
    }

    public final void onCreate() {
        super.onCreate();
        m_singleton = this;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            Config.APP_VERSION = pi.versionName;
            Config.APP_VERSION_ID = pi.versionCode;
        } catch (NameNotFoundException e) {
        }
        try {
            boolean z;
            if (Class.forName("net.hockeyapp.android.UpdateActivity") != null) {
                z = true;
            } else {
                z = false;
            }
            this.m_bOverTheAirVersion = z;
        } catch (Exception e2) {
        }
        m_CrashHandler.register();
        registerReceiver(this.m_umqdbIntentReceiver, new IntentFilter(SteamUmqCommunicationService.INTENT_ACTION));
        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().setAcceptCookie(true);
        SteamWebApi.ResetAllCookies();
        this.m_diskCacheWeb = new WebCache(getBaseContext().getDir("cache_w", 0));
        this.m_diskCacheIndefinite = new IndefiniteCache(getBaseContext().getDir("cache_i", 0));
        this.m_friendInfoDB = new FriendInfoDB();
        this.m_groupInfoDB = new GroupInfoDB();
        this.m_settingInfoDB = new SettingInfoDB();
        this.m_steamDBConnection = new SteamDBServiceConnection();
        GetInstance().bindService(new Intent(GetInstance(), SteamDBService.class), this.m_steamDBConnection, 1);
        LoginActivity.PreemptivelyLoginBasedOnCachedLoginDocument();
        SteamWebApi.SyncAllCookies();
    }

    public final void onTerminate() {
        unregisterReceiver(this.m_umqdbIntentReceiver);
        super.onTerminate();
    }

    public final void onLowMemory() {
        super.onLowMemory();
    }

    public final void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public PendingIntent GetIntentToChatWithSteamID(String steamid) {
        Intent intentBringToForeground = new Intent();
        intentBringToForeground.setAction("android.intent.action.MAIN");
        intentBringToForeground.setData(Uri.parse(SteamUriHandler.MOBILE_PROTOCOL + Command.chat.toString() + "?" + CommandProperty.steamid.toString() + "=" + steamid));
        intentBringToForeground.putExtra("steamid", steamid);
        intentBringToForeground.setFlags(402653184);
        intentBringToForeground.setClass(getApplicationContext(), ChatActivity.class);
        return PendingIntent.getActivity(getApplicationContext(), steamid.hashCode(), intentBringToForeground, 0);
    }

    public PendingIntent GetIntentForUriString(String uri) {
        return GetIntentForUriString(Uri.parse(uri));
    }

    public PendingIntent GetIntentForUriString(Uri uri) {
        return GetIntentForUriString(uri, 0);
    }

    public PendingIntent GetIntentForUriString(Uri uri, int title_resid) {
        Intent intentBringToForeground = new Intent();
        intentBringToForeground.setAction("android.intent.action.VIEW");
        intentBringToForeground.setData(uri);
        intentBringToForeground.setFlags(402653184);
        intentBringToForeground.setClass(getApplicationContext(), SteamMobileUriActivity.class);
        if (title_resid != 0) {
            intentBringToForeground.putExtra("title_resid", title_resid);
        }
        return PendingIntent.getActivity(getApplicationContext(), uri.hashCode(), intentBringToForeground, 0);
    }

    public PendingIntent GetIntentForActivityClass(Class<?> activityClass) {
        Intent intentBringToForeground = new Intent();
        intentBringToForeground.setClass(getApplicationContext(), activityClass);
        intentBringToForeground.setFlags(805306368);
        return PendingIntent.getActivity(getApplicationContext(), 0, intentBringToForeground, 0);
    }

    public PendingIntent GetIntentForActivityClassFromService(Class<?> activityClass) {
        Intent intentBringToForeground = new Intent();
        intentBringToForeground.setClass(getApplicationContext(), activityClass);
        intentBringToForeground.setFlags(805306368);
        return PendingIntent.getActivity(getApplicationContext(), 0, intentBringToForeground, 0);
    }
}
