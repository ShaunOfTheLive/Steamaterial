package com.valvesoftware.android.steam.community.activity;

import android.os.Bundle;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamWebApi;

public class CommunityGroupsActivity extends FragmentActivityWithNavigationSupport {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.community_groups_activity);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onResume() {
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
        super.onResume();
    }
}
