package com.valvesoftware.android.steam.community.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.fragment.SearchFriendListFragment;

public class SearchFriendsActivity extends FragmentActivityWithNavigationSupport {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_friends_activity);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onResume() {
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
        super.onResume();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Fragment communityList = ActivityHelper.GetFragmentForActivityById(this, R.id.community_list);
        if (communityList != null && (communityList instanceof SearchFriendListFragment)) {
            ((SearchFriendListFragment) communityList).SwitchConfiguration(intent.getStringExtra("query"));
        }
    }
}
