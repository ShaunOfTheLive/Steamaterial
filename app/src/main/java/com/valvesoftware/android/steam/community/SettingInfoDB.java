package com.valvesoftware.android.steam.community;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build.VERSION;
import com.valvesoftware.android.steam.community.SettingInfo.AccessRight;
import com.valvesoftware.android.steam.community.SettingInfo.DateConverter;
import com.valvesoftware.android.steam.community.SettingInfo.RadioSelectorItem;
import com.valvesoftware.android.steam.community.SettingInfo.SettingType;
import com.valvesoftware.android.steam.community.SettingInfo.Transaction;
import com.valvesoftware.android.steam.community.SettingInfo.UpdateListener;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_SETTINGCHANGE_DATA;
import com.valvesoftware.android.steam.community.fragment.ChatFragment.Layout;
import java.util.ArrayList;

public class SettingInfoDB extends BroadcastReceiver {
    public static final String KEY_CHATS_LAYOUT = "chats_layout";
    public static final String KEY_CHATS_LINKS_NONSTEAM = "chats_links_nonsteam";
    public static final String KEY_CHATS_RECENT_SECONDS = "chats_recent_seconds";
    public static final String KEY_CHATS_TIMESTAMP_SECONDS = "chats_timestamp_seconds";
    public static final String KEY_HW_ACCELERATION = "hw_acceleration";
    public static final String KEY_NOTIFICATIONS_IM = "notifications_im2";
    public static final String KEY_NOTIFICATIONS_ONGOING = "notifications_ongoing";
    public static final String KEY_NOTIFICATIONS_RINGTONE = "notifications_ringtone";
    public static final String KEY_NOTIFICATIONS_SOUND = "notifications_sound";
    public static final String KEY_NOTIFICATIONS_VIBRATE = "notifications_vibrate";
    public static final String KEY_PERSONAL_DOB = "personal_dob";
    public static final String KEY_SSL_UNTRUSTED_PROMPT = "ssl_untrusted_prompt";
    public static final int SETTING_NOTIFY_ALL = -1;
    public static final int SETTING_NOTIFY_FIRST = 0;
    public static final int SETTING_NOTIFY_NEVER = 1;
    public static final String URL_SETTINGS_ONLINE;
    private final String m_className;
    public SettingInfo m_settingChatsAlertLinks;
    public SettingInfo m_settingChatsLayout;
    public SettingInfo m_settingChatsMarkRead;
    public SettingInfo m_settingChatsRecent;
    public SettingInfo m_settingChatsTimestamp;
    public SettingInfo m_settingDOB;
    public SettingInfo m_settingForegroundService;
    public SettingInfo m_settingHardwareAcceleration;
    public SettingInfo m_settingNotificationsIMs2;
    private SettingInfo m_settingRing;
    private SettingInfo m_settingSound;
    public SettingInfo m_settingSslUntrustedPrompt;
    private SettingInfo m_settingVibrate;
    private ArrayList<SettingInfo> m_settingsList;

    static {
        URL_SETTINGS_ONLINE = "steammobile://opencategoryurl?url=" + Config.URL_COMMUNITY_BASE_INSECURE + "/mobilesettings/GetManifest/v0001";
    }

    SettingInfoDB() {
        int i;
        this.m_settingsList = new ArrayList();
        this.m_className = getClass().getName();
        SteamCommunityApplication.GetInstance().registerReceiver(this, new IntentFilter(this.m_className));
        SettingInfo setting = new SettingInfo();
        setting.m_resid = 2131165321;
        setting.m_resid_detailed = 2131165322;
        setting.m_defaultValue = SteamCommunityApplication.GetInstance().getString(setting.m_resid_detailed);
        setting.m_type = SettingType.UNREADMSG;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingChatsMarkRead = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165325;
        RadioSelectorItem[] radios = new RadioSelectorItem[5];
        radios[0] = new RadioSelectorItem();
        radios[0].value = 604800;
        radios[0].resid_text = 2131165326;
        radios[1] = new RadioSelectorItem();
        radios[1].value = 172800;
        radios[1].resid_text = 2131165327;
        radios[2] = new RadioSelectorItem();
        radios[2].value = 86400;
        radios[2].resid_text = 2131165328;
        radios[3] = new RadioSelectorItem();
        radios[3].value = 3600;
        radios[3].resid_text = 2131165329;
        radios[4] = new RadioSelectorItem();
        radios[4].value = 0;
        radios[4].resid_text = 2131165330;
        setting.m_extraData = radios;
        setting.m_key = KEY_CHATS_RECENT_SECONDS;
        setting.m_defaultValue = String.valueOf(172800);
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingChatsRecent = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165331;
        radios = new RadioSelectorItem[3];
        radios[0] = new RadioSelectorItem();
        radios[0].value = Layout.Bubbles.ordinal();
        radios[0].resid_text = 2131165332;
        radios[1] = new RadioSelectorItem();
        radios[1].value = Layout.BubblesLeft.ordinal();
        radios[1].resid_text = 2131165333;
        radios[2] = new RadioSelectorItem();
        radios[2].value = Layout.TextOnly.ordinal();
        radios[2].resid_text = 2131165334;
        setting.m_extraData = radios;
        setting.m_key = KEY_CHATS_LAYOUT;
        setting.m_defaultValue = String.valueOf(Layout.Bubbles.ordinal());
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingChatsLayout = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165335;
        radios = new RadioSelectorItem[5];
        radios[0] = new RadioSelectorItem();
        radios[0].value = 86400;
        radios[0].resid_text = 2131165336;
        radios[1] = new RadioSelectorItem();
        radios[1].value = 3600;
        radios[1].resid_text = 2131165337;
        radios[2] = new RadioSelectorItem();
        radios[2].value = 900;
        radios[2].resid_text = 2131165338;
        radios[3] = new RadioSelectorItem();
        radios[3].value = 60;
        radios[3].resid_text = 2131165339;
        radios[4] = new RadioSelectorItem();
        radios[4].value = 0;
        radios[4].resid_text = 2131165340;
        setting.m_extraData = radios;
        setting.m_key = KEY_CHATS_TIMESTAMP_SECONDS;
        setting.m_defaultValue = String.valueOf(900);
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingChatsTimestamp = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165341;
        setting.m_resid_detailed = 2131165342;
        setting.m_key = KEY_CHATS_LINKS_NONSTEAM;
        setting.m_defaultValue = "1";
        setting.m_type = SettingType.CHECK;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingChatsAlertLinks = setting;
        this.m_settingsList.add(setting);
        if (VERSION.SDK_INT >= 11) {
            setting = new SettingInfo();
            setting.m_resid = 2131165346;
            setting.m_resid_detailed = 2131165347;
            setting.m_key = KEY_HW_ACCELERATION;
            setting.m_defaultValue = "1";
            setting.m_type = SettingType.CHECK;
            setting.m_access = AccessRight.VALID_ACCOUNT;
            this.m_settingHardwareAcceleration = setting;
            this.m_settingsList.add(setting);
        }
        setting = new SettingInfo();
        setting.m_resid = 2131165261;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165264;
        setting.m_resid_detailed = 2131165265;
        setting.m_defaultValue = URL_SETTINGS_ONLINE;
        setting.m_type = SettingType.URI;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165262;
        setting.m_resid_detailed = 2131165263;
        setting.m_key = KEY_PERSONAL_DOB;
        setting.m_defaultValue = "";
        setting.m_type = SettingType.DATE;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        setting.m_pUpdateListener = new UpdateListener() {
            public void OnSettingInfoValueUpdate(SettingInfo si, String value, Transaction tr) {
                SteamWebApi.SetCookie2("dob", DateConverter.makeUnixTime(value));
                tr.markCookiesForSync();
            }
        };
        this.m_settingDOB = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165243;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165244;
        setting.m_resid_detailed = 2131165245;
        setting.m_key = KEY_NOTIFICATIONS_ONGOING;
        setting.m_defaultValue = "1";
        setting.m_type = SettingType.CHECK;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        setting.m_pUpdateListener = new UpdateListener() {
            public void OnSettingInfoValueUpdate(SettingInfo si, String value, Transaction tr) {
                REQ_ACT_SETTINGCHANGE_DATA obj = new REQ_ACT_SETTINGCHANGE_DATA();
                obj.sSteamID = SteamWebApi.GetLoginSteamID();
                obj.sSettingKey = KEY_NOTIFICATIONS_ONGOING;
                obj.sNewValue = value;
                SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_SETTINGCHANGE, obj);
            }
        };
        this.m_settingForegroundService = setting;
        this.m_settingsList.add(setting);
        boolean bC2DMSupported = C2DMessaging.isSupported();
        setting = new SettingInfo();
        setting.m_resid = 2131165246;
        if (bC2DMSupported) {
            i = 3;
        } else {
            i = 2;
        }
        radios = new RadioSelectorItem[i];
        radios[0] = new RadioSelectorItem();
        radios[0].value = 2;
        radios[0].resid_text = 2131165247;
        int ridx = 0 + 1;
        if (bC2DMSupported) {
            radios[ridx] = new RadioSelectorItem();
            radios[ridx].value = 1;
            radios[ridx].resid_text = 2131165248;
            ridx++;
        }
        radios[ridx] = new RadioSelectorItem();
        radios[ridx].value = 0;
        radios[ridx].resid_text = 2131165249;
        setting.m_extraData = radios;
        setting.m_key = KEY_NOTIFICATIONS_IM;
        setting.m_defaultValue = bC2DMSupported ? "1" : "0";
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        setting.m_pUpdateListener = new UpdateListener() {
            public void OnSettingInfoValueUpdate(SettingInfo si, String value, Transaction tr) {
                REQ_ACT_SETTINGCHANGE_DATA obj = new REQ_ACT_SETTINGCHANGE_DATA();
                obj.sSteamID = SteamWebApi.GetLoginSteamID();
                obj.sSettingKey = KEY_NOTIFICATIONS_IM;
                obj.sNewValue = value;
                SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_SETTINGCHANGE, obj);
            }
        };
        this.m_settingNotificationsIMs2 = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165250;
        setting.m_key = KEY_NOTIFICATIONS_RINGTONE;
        setting.m_defaultValue = "android.resource://com.valvesoftware.android.steam.community/raw/m";
        setting.m_type = SettingType.RINGTONESELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingRing = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165253;
        radios = new RadioSelectorItem[3];
        radios[0] = new RadioSelectorItem();
        radios[0].value = -1;
        radios[0].resid_text = 2131165254;
        radios[1] = new RadioSelectorItem();
        radios[1].value = 0;
        radios[1].resid_text = 2131165255;
        radios[2] = new RadioSelectorItem();
        radios[2].value = 1;
        radios[2].resid_text = 2131165256;
        setting.m_extraData = radios;
        setting.m_key = KEY_NOTIFICATIONS_SOUND;
        setting.m_defaultValue = String.valueOf(SETTING_NOTIFY_FIRST);
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingSound = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165257;
        radios = new RadioSelectorItem[3];
        radios[0] = new RadioSelectorItem();
        radios[0].value = -1;
        radios[0].resid_text = 2131165258;
        radios[1] = new RadioSelectorItem();
        radios[1].value = 0;
        radios[1].resid_text = 2131165259;
        radios[2] = new RadioSelectorItem();
        radios[2].value = 1;
        radios[2].resid_text = 2131165260;
        setting.m_extraData = radios;
        setting.m_key = KEY_NOTIFICATIONS_VIBRATE;
        setting.m_defaultValue = String.valueOf(SETTING_NOTIFY_FIRST);
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = AccessRight.VALID_ACCOUNT;
        this.m_settingVibrate = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165266;
        radios = new RadioSelectorItem[3];
        radios[0] = new RadioSelectorItem();
        radios[0].value = 1;
        radios[0].resid_text = 2131165267;
        radios[1] = new RadioSelectorItem();
        radios[1].value = 0;
        radios[1].resid_text = 2131165268;
        radios[2] = new RadioSelectorItem();
        radios[2].value = -1;
        radios[2].resid_text = 2131165269;
        setting.m_extraData = radios;
        setting.m_key = KEY_SSL_UNTRUSTED_PROMPT;
        setting.m_defaultValue = String.valueOf(SETTING_NOTIFY_NEVER);
        setting.m_type = SettingType.RADIOSELECTOR;
        setting.m_access = VERSION.SDK_INT < 8 ? AccessRight.CODE : AccessRight.NONE;
        this.m_settingSslUntrustedPrompt = setting;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165311;
        setting.m_access = AccessRight.USER;
        this.m_settingsList.add(setting);
        setting = new SettingInfo();
        setting.m_resid = 2131165312;
        setting.m_defaultValue = Config.APP_VERSION + " / " + Config.APP_VERSION_ID;
        setting.m_type = SettingType.MARKET;
        setting.m_access = AccessRight.USER;
        this.m_settingsList.add(setting);
    }

    public ArrayList<SettingInfo> GetSettingsList() {
        return this.m_settingsList;
    }

    public void onReceive(Context context, Intent intent) {
    }

    public void configureNotificationObject(Notification n, boolean bFirstNotification) {
        Context ctx = SteamCommunityApplication.GetInstance().getApplicationContext();
        RadioSelectorItem sound = this.m_settingSound.getRadioSelectorItemValue(ctx);
        RadioSelectorItem vibro = this.m_settingVibrate.getRadioSelectorItemValue(ctx);
        n.defaults = 4;
        boolean bSoundEnabled = sound.value == -1 || (sound.value == 0 && bFirstNotification);
        if (bSoundEnabled) {
            try {
                n.sound = Uri.parse(this.m_settingRing.getValue(ctx));
            } catch (Exception e) {
                n.sound = Uri.parse(this.m_settingRing.m_defaultValue);
            }
        }
        boolean bVibroEnabled = vibro.value == -1 || (vibro.value == 0 && bFirstNotification);
        if (bVibroEnabled) {
            n.vibrate = new long[]{0, (long) 200, (long) 200, (long) 200, (long) 1000};
        }
    }
}
