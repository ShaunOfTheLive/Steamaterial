package com.valvesoftware.android.steam.community;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class C2DMReceiver extends BroadcastReceiver {
    public final void onReceive(Context context, Intent intent) {
        C2DMReceiverService.runIntentInService(context, intent);
        setResult(-1, null, null);
    }
}
