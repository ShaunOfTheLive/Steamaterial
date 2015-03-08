package com.valvesoftware.android.steam.community.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Menu;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SettingInfo;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.fragment.BasePresentationListFragmentWithSearch;
import com.valvesoftware.android.steam.community.fragment.ChatFragment;
import com.valvesoftware.android.steam.community.fragment.NavigationFragment;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment;
import java.util.ArrayList;

public class FragmentActivityWithNavigationSupport extends FragmentActivity {
    private static ArrayList<FragmentActivityWithNavigationSupport> s_allActivities;

    public void onBackPressed() {
        NavigationFragment frag = ActivityHelper.GetNavigationFragmentForActivity(this);
        if (frag == null || !frag.overrideActivityOnBackPressed()) {
            Fragment communityList = ActivityHelper.GetFragmentForActivityById(this, R.id.community_list);
            if (communityList == null || !(communityList instanceof BasePresentationListFragmentWithSearch) || !((BasePresentationListFragmentWithSearch) communityList).overrideActivityOnBackPressed()) {
                Fragment chatFragment = ActivityHelper.GetFragmentForActivityById(this, R.id.chat);
                if (chatFragment == null || !(chatFragment instanceof ChatFragment) || !((ChatFragment) chatFragment).overrideActivityOnBackPressed()) {
                    try {
                        super.onBackPressed();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case 84:
                TitlebarFragment frag = ActivityHelper.GetTitlebarFragmentForActivity(this);
                if (frag != null && frag.overrideActivityOnSearchPressed()) {
                    return true;
                }
        }
        return super.onKeyDown(keycode, e);
    }

    static {
        s_allActivities = new ArrayList();
    }

    public static void finishAll() {
        finishAllExceptOne(null);
    }

    public static void finishAllExceptOne(FragmentActivityWithNavigationSupport actSkip) {
        while (!s_allActivities.isEmpty()) {
            FragmentActivityWithNavigationSupport act = (FragmentActivityWithNavigationSupport) s_allActivities.remove(0);
            if (act != actSkip) {
                act.finish();
            }
        }
    }

    public static boolean hasValidActivities() {
        return !s_allActivities.isEmpty();
    }

    private boolean shouldBeExcludedFromTracking() {
        return this instanceof LoginActivity;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!shouldBeExcludedFromTracking()) {
            s_allActivities.add(this);
        }
        SettingInfo setting = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingHardwareAcceleration;
        if (setting != null && !(this instanceof SteamMobileUriActivity) && setting.getBooleanValue(SteamCommunityApplication.GetInstance().getApplicationContext())) {
            getWindow().setFlags(16777216, 16777216);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (!shouldBeExcludedFromTracking()) {
            s_allActivities.remove(this);
        }
    }

    protected void onResume() {
        super.onResume();
        if (s_allActivities.remove(this)) {
            s_allActivities.add(this);
        }
        int k = 0;
        while (k < s_allActivities.size() - 15) {
            FragmentActivityWithNavigationSupport act = (FragmentActivityWithNavigationSupport) s_allActivities.get(k);
            if (!(act instanceof CommunityActivity) && !(act instanceof CommunityGroupsActivity) && !(act instanceof SearchFriendsActivity) && !(act instanceof SearchGroupsActivity) && !(act instanceof SettingsActivity) && !(act instanceof LoginActivity)) {
                s_allActivities.remove(k);
                int preFinishCount = s_allActivities.size();
                act.finish();
                if (s_allActivities.size() <= preFinishCount) {
                    k--;
                }
            }
            k++;
        }
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        NavigationFragment nav = ActivityHelper.GetNavigationFragmentForActivity(this);
        if (nav != null) {
            nav.onNavActivationButtonClicked();
        }
        return false;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        NavigationFragment frag = ActivityHelper.GetNavigationFragmentForActivity(this);
        if (frag != null) {
            frag.overrideActivityOnConfigurationChanged();
        }
    }
}
