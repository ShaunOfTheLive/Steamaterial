package com.valvesoftware.android.steam.community;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import java.io.IOException;

public abstract class C2DMReceiverService extends IntentService {
    private static final String C2DM_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    private static final String C2DM_RETRY = "com.google.android.c2dm.intent.RETRY";
    public static final String ERR_ACCOUNT_MISSING = "ACCOUNT_MISSING";
    public static final String ERR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ERR_INVALID_PARAMETERS = "INVALID_PARAMETERS";
    public static final String ERR_INVALID_SENDER = "INVALID_SENDER";
    public static final String ERR_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";
    public static final String ERR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";
    public static final String ERR_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_REGISTRATION_ID = "registration_id";
    public static final String EXTRA_UNREGISTERED = "unregistered";
    public static final String REGISTRATION_CALLBACK_INTENT = "com.google.android.c2dm.intent.REGISTRATION";
    private static final String WAKELOCK_KEY = "C2DM_LIB";
    private final String senderId;

    public abstract void onError(Context context, String str);

    protected abstract void onMessage(Context context, Intent intent);

    public C2DMReceiverService(String senderId) {
        super(senderId);
        this.senderId = senderId;
    }

    public void onRegistered(Context context, String registrationId) throws IOException {
    }

    public void onUnregistered(Context context) {
    }

    public final void onHandleIntent(Intent intent) {
        WakeLock myWakeLock = null;
        try {
            Context context = getApplicationContext();
            PowerManager pm = (PowerManager) context.getSystemService("power");
            if (pm != null) {
                myWakeLock = pm.newWakeLock(1, WAKELOCK_KEY);
                if (myWakeLock != null) {
                    myWakeLock.acquire();
                }
            }
            if (intent.getAction().equals(REGISTRATION_CALLBACK_INTENT)) {
                handleRegistration(context, intent);
            } else if (intent.getAction().equals(C2DM_INTENT)) {
                onMessage(context, intent);
            } else if (intent.getAction().equals(C2DM_RETRY)) {
                C2DMessaging.register(context, this.senderId);
            }
            if (myWakeLock != null) {
                myWakeLock.release();
            }
        } catch (Throwable th) {
            if (myWakeLock != null) {
                myWakeLock.release();
            }
        }
    }

    static void runIntentInService(Context context, Intent intent) {
        intent.setClassName(context, context.getPackageName() + ".C2DMProcessor");
        context.startService(intent);
    }

    private void handleRegistration(Context context, Intent intent) {
        String registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID);
        String error = intent.getStringExtra(EXTRA_ERROR);
        if (intent.getStringExtra(EXTRA_UNREGISTERED) != null) {
            C2DMessaging.clearRegistrationId(context);
            C2DMessaging.resetBackoff(context);
            onUnregistered(context);
        } else if (error != null) {
            C2DMessaging.clearRegistrationId(context);
            onError(context, error);
            if (ERR_SERVICE_NOT_AVAILABLE.equals(error)) {
                long backoffTimeMs = C2DMessaging.getBackoff(context);
                ((AlarmManager) context.getSystemService("alarm")).set(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER, backoffTimeMs, PendingIntent.getBroadcast(context, 0, new Intent(C2DM_RETRY), 0));
                C2DMessaging.setBackoff(context, backoffTimeMs * 2);
            }
        } else {
            try {
                C2DMessaging.setRegistrationId(context, registrationId);
                C2DMessaging.resetBackoff(context);
                onRegistered(context, registrationId);
            } catch (IOException e) {
            }
        }
    }
}
