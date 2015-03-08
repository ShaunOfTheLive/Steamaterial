package com.valvesoftware.android.steam.community;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.valvesoftware.android.steam.community.SteamCommunityApplication.UriNotification;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import java.io.IOException;

public class C2DMProcessor extends C2DMReceiverService {
    static final String C2DM_EXTRA_MSG_IM_ACTION = "a";
    static final String C2DM_EXTRA_MSG_IM_ID = "id";
    static final String C2DM_EXTRA_MSG_IM_TEXT = "s2";
    static final String C2DM_EXTRA_MSG_IM_TITLE = "s1";
    static final String C2DM_EXTRA_MSG_TYPE = "type";
    static final String C2DM_NOTIFICATION_IM = "im";
    static final String C2DM_SENDER = "valvesoftwaresteampowered@gmail.com";
    public static final String INTENT_C2DM_REGISTRATION_READY = "com.valvesoftware.android.steam.community.INTENT_C2DM_REGISTRATION_READY";
    public static final String INTENT_C2DM_REGISTRATION_READY_EXTRA_ID = "registration.id";
    static final String PREFERENCE = "com.valvesoftware.android.steam.community.c2dm";
    static final String STORED_IM_ID = "im_id";

    public C2DMProcessor() {
        super(C2DM_SENDER);
    }

    public void onError(Context context, String errorId) {
    }

    protected void onMessage(Context context, Intent intent) {
        if (C2DM_NOTIFICATION_IM.equals(intent.getExtras().getString(C2DM_EXTRA_MSG_TYPE))) {
            UriNotification n = new UriNotification();
            try {
                int intValue;
                String sNotificationId = intent.getStringExtra(C2DM_EXTRA_MSG_IM_ID);
                if (sNotificationId != null) {
                    intValue = Integer.valueOf(sNotificationId).intValue();
                } else {
                    intValue = 0;
                }
                n.id = intValue;
            } catch (Exception e) {
            }
            n.title = intent.getStringExtra(C2DM_EXTRA_MSG_IM_TITLE);
            n.text = intent.getStringExtra(C2DM_EXTRA_MSG_IM_TEXT);
            int iAction = 0;
            while (true) {
                String sAction = intent.getStringExtra(C2DM_EXTRA_MSG_IM_ACTION + iAction);
                if (sAction != null) {
                    if (n.setActionInfo(sAction)) {
                        break;
                    }
                    iAction++;
                } else {
                    break;
                }
            }
            n.setDefaultActionInfo();
            if (n.id > 0 && n.text != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFERENCE, 0);
                if (n.id > prefs.getInt(STORED_IM_ID, 0)) {
                    onServerPushNotification(context, n);
                    Editor editor = prefs.edit();
                    editor.putInt(STORED_IM_ID, n.id);
                    editor.commit();
                }
            }
        }
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
    }

    public static void refreshAppC2DMRegistrationState(Context context) {
        if (!C2DMessaging.getRegistrationId(context).equals("")) {
            return;
        }
        if (true) {
            C2DMessaging.register(context, C2DM_SENDER);
        } else {
            C2DMessaging.unregister(context);
        }
    }

    public void onRegistered(Context context, String registrationId) throws IOException {
        Intent intent = new Intent(INTENT_C2DM_REGISTRATION_READY);
        intent.putExtra(INTENT_C2DM_REGISTRATION_READY_EXTRA_ID, registrationId);
        SteamCommunityApplication app = SteamCommunityApplication.GetInstance();
        if (app != null) {
            SteamDBService db = app.GetSteamDB();
            if (db != null) {
                db.sendBroadcast(intent);
            }
        }
    }

    private void onServerPushNotification(Context context, UriNotification n) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService("notification");
            if (nm != null) {
                Notification notification = new Notification(n.actionDrawable, n.text, System.currentTimeMillis());
                notification.flags |= 16;
                SteamCommunityApplication.GetInstance().GetSettingInfoDB().configureNotificationObject(notification, true);
                String name = n.title;
                if (name == null) {
                    name = context.getText(R.string.Steam).toString();
                }
                String text = n.text;
                if (text == null) {
                    text = "";
                }
                notification.setLatestEventInfo(context, name, text, n.actionPendingIntent);
                nm.notify(UriNotification.NOTIFICATION_TAG, n.id, notification);
            }
        } catch (Exception e) {
        }
    }

    public static int getIMID(Context context) {
        return context.getSharedPreferences(PREFERENCE, 0).getInt(STORED_IM_ID, 0);
    }
}
