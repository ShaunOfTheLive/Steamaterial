package com.valvesoftware.android.steam.community;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Build.VERSION;

public class C2DMessaging {
    public static final String BACKOFF = "backoff";
    private static final long DEFAULT_BACKOFF = 30000;
    public static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
    public static final String EXTRA_SENDER = "sender";
    public static final String GSF_PACKAGE = "com.google.android.gsf";
    public static final String LAST_REGISTRATION_CHANGE = "last_registration_change";
    private static final long MAXIMUM_BACKOFF = 7200000;
    static final String PREFERENCE = "com.google.android.c2dm";
    public static final String REQUEST_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTER";
    public static final String REQUEST_UNREGISTRATION_INTENT = "com.google.android.c2dm.intent.UNREGISTER";
    public static final String STORED_REGISTRATION_ID = "c2dm_registration_id_2";
    public static final String TAG = "C2DM";

    public static boolean isSupported() {
        boolean bResult = VERSION.SDK_INT >= 8;
        return !bResult ? bResult : bResult;
    }

    public static void register(Context context, String senderId) {
        if (isSupported()) {
            try {
                Intent registrationIntent = new Intent(REQUEST_REGISTRATION_INTENT);
                registrationIntent.setPackage(GSF_PACKAGE);
                registrationIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context, 0, new Intent(), 0));
                registrationIntent.putExtra(EXTRA_SENDER, senderId);
                context.startService(registrationIntent);
            } catch (Exception e) {
            }
        }
    }

    public static void unregister(Context context) {
        if (isSupported()) {
            try {
                Intent regIntent = new Intent(REQUEST_UNREGISTRATION_INTENT);
                regIntent.setPackage(GSF_PACKAGE);
                regIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context, 0, new Intent(), 0));
                context.startService(regIntent);
            } catch (Exception e) {
            }
        }
    }

    public static String getRegistrationId(Context context) {
        return !isSupported() ? "" : context.getSharedPreferences(PREFERENCE, 0).getString(STORED_REGISTRATION_ID, "");
    }

    public static long getLastRegistrationChange(Context context) {
        return context.getSharedPreferences(PREFERENCE, 0).getLong(LAST_REGISTRATION_CHANGE, 0);
    }

    static long getBackoff(Context context) {
        return Math.min(context.getSharedPreferences(PREFERENCE, 0).getLong(BACKOFF, DEFAULT_BACKOFF), MAXIMUM_BACKOFF);
    }

    static void setBackoff(Context context, long backoff) {
        backoff = Math.min(backoff, MAXIMUM_BACKOFF);
        Editor editor = context.getSharedPreferences(PREFERENCE, 0).edit();
        editor.putLong(BACKOFF, backoff);
        editor.commit();
    }

    static void resetBackoff(Context context) {
        setBackoff(context, DEFAULT_BACKOFF);
    }

    static void clearRegistrationId(Context context) {
        Editor editor = context.getSharedPreferences(PREFERENCE, 0).edit();
        editor.putString(STORED_REGISTRATION_ID, "");
        editor.putLong(LAST_REGISTRATION_CHANGE, System.currentTimeMillis());
        editor.commit();
    }

    static void setRegistrationId(Context context, String registrationId) {
        Editor editor = context.getSharedPreferences(PREFERENCE, 0).edit();
        editor.putString(STORED_REGISTRATION_ID, registrationId);
        editor.commit();
    }
}
