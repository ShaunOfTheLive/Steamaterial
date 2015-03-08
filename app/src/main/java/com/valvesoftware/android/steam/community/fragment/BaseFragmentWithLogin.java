package com.valvesoftware.android.steam.community.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginAction;

public class BaseFragmentWithLogin {
    private OnClickListener loginButtonListener;
    private Fragment m_fragment;
    private LinearLayout m_loggedIn;
    private Button m_loginButton;
    private RelativeLayout m_notLoggedIn;

    BaseFragmentWithLogin(Fragment frag) {
        this.m_loginButton = null;
        this.m_loggedIn = null;
        this.m_notLoggedIn = null;
        this.m_fragment = null;
        this.loginButtonListener = new OnClickListener() {
            public void onClick(View v) {
                ActivityHelper.PresentLoginActivity(BaseFragmentWithLogin.this.getActivity(), LoginAction.LOGIN_DEFAULT);
            }
        };
        this.m_fragment = frag;
    }

    private Activity getActivity() {
        return this.m_fragment.getActivity();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        this.m_loginButton = (Button) getActivity().findViewById(R.id.login);
        this.m_loggedIn = (LinearLayout) getActivity().findViewById(R.id.loggedIn);
        this.m_notLoggedIn = (RelativeLayout) getActivity().findViewById(R.id.notLoggedIn);
        this.m_loginButton.setOnClickListener(this.loginButtonListener);
    }

    private void UpdateListVisibilityToMatchLoggedInState() {
        if (this.m_notLoggedIn != null && this.m_loggedIn != null) {
            if (SteamWebApi.IsLoggedIn()) {
                this.m_notLoggedIn.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                this.m_loggedIn.setVisibility(0);
                return;
            }
            this.m_loggedIn.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
            this.m_notLoggedIn.setVisibility(0);
        }
    }

    public void onResume() {
        UpdateListVisibilityToMatchLoggedInState();
    }

    public void UpdateGlobalInformationPreSort() {
        UpdateListVisibilityToMatchLoggedInState();
    }
}
