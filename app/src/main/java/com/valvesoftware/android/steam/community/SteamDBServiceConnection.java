package com.valvesoftware.android.steam.community;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.valvesoftware.android.steam.community.SteamDBService.SteamDBTempBinder;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import java.util.ArrayList;
import java.util.Iterator;

class SteamDBServiceConnection implements ServiceConnection {
    private ArrayList<RequestBase> m_pendingReqs;
    private SteamDBService m_steamDBBinder;

    SteamDBServiceConnection() {
        this.m_steamDBBinder = null;
        this.m_pendingReqs = new ArrayList();
    }

    protected final String TAG() {
        return getClass().getSimpleName();
    }

    public void SubmitRequest(RequestBase request) {
        if (this.m_steamDBBinder != null) {
            this.m_steamDBBinder.SubmitRequest(request);
        } else {
            this.m_pendingReqs.add(request);
        }
    }

    SteamDBService GetService() {
        return this.m_steamDBBinder;
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        this.m_steamDBBinder = ((SteamDBTempBinder) service).getService();
        Iterator i$ = this.m_pendingReqs.iterator();
        while (i$.hasNext()) {
            this.m_steamDBBinder.SubmitRequest((RequestBase) i$.next());
        }
        this.m_pendingReqs.clear();
    }

    public void onServiceDisconnected(ComponentName name) {
        this.m_steamDBBinder = null;
    }
}
