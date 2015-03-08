package com.valvesoftware.android.steam.community.activity;

import android.content.Context;
import android.os.Bundle;
import com.valvesoftware.android.steam.community.Config;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginAction;

public class CommunityActivity extends FragmentActivityWithNavigationSupport {
    private static long s_lastCheckForUpdates;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.community_activity);
        if (!SteamWebApi.IsLoggedIn()) {
            ActivityHelper.PresentLoginActivity(this, LoginAction.LOGIN_DEFAULT);
        }
    }

    static {
        s_lastCheckForUpdates = 0;
    }

    private void checkForUpdates() {
        if (SteamCommunityApplication.GetInstance().IsOverTheAirVersion() && Config.APP_VERSION_ID != 0) {
            long curTime = System.currentTimeMillis();
            if (s_lastCheckForUpdates == 0 || curTime - s_lastCheckForUpdates >= 1800000) {
                s_lastCheckForUpdates = curTime;
                try {
                    Class.forName("net.hockeyapp.android.UpdateActivity").getDeclaredField("iconDrawableId").setInt(null, R.drawable.steam);
                    Class<?> clsTask = Class.forName("net.hockeyapp.android.CheckUpdateTask");
                    Object objTask = clsTask.getDeclaredConstructor(new Class[]{Context.class, String.class}).newInstance(new Object[]{this, Config.URL_MOBILE_UPDATE});
                    clsTask.getSuperclass().getDeclaredMethod("execute", new Class[]{Object[].class}).invoke(objTask, new Object[]{(String) null});
                } catch (Exception e) {
                }
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onResume() {
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
        super.onResume();
        checkForUpdates();
    }
}
