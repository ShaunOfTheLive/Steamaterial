package com.valvesoftware.android.steam.community.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.fragment.SettingsFragment;

public class SettingsActivity extends FragmentActivityWithNavigationSupport {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onResume() {
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
        super.onResume();
    }

    public void onActivityResult(int arg0, int arg1, Intent arg2) {
        super.onActivityResult(arg0, arg1, arg2);
        Fragment frag = ActivityHelper.GetFragmentForActivityById(this, R.id.settings_fragment);
        if (frag != null && (frag instanceof SettingsFragment)) {
            ((SettingsFragment) frag).onFragmentActivityResult(arg0, arg1, arg2);
        }
    }
}
