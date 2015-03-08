package com.valvesoftware.android.steam.community.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.ToggleButton;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBResponseListener;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginAction;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginChangedListener;
import com.valvesoftware.android.steam.community.fragment.NavigationFragment;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment;
import java.lang.ref.WeakReference;

public class ActivityHelper {
    private static AlertDialog m_commErrorDlg;
    private final SteamDBCallback m_intentReceiver;
    private Activity m_owner;

    class SteamDBCallback extends BroadcastReceiver {
        SteamDBCallback() {
        }

        public void onReceive(Context context, Intent intent) {
            ((SteamDBResponseListener) ActivityHelper.this.m_owner).OnRequestCompleted(Integer.valueOf(intent.getIntExtra("intent_id", -1)));
        }
    }

    protected final String TAG() {
        return this.m_owner.getClass().getName();
    }

    private ActivityHelper() {
        this.m_intentReceiver = new SteamDBCallback();
        this.m_owner = null;
    }

    ActivityHelper(Activity owner) {
        this.m_intentReceiver = new SteamDBCallback();
        this.m_owner = null;
        this.m_owner = owner;
    }

    public void onCreate(Bundle savedInstanceState) {
        this.m_owner.registerReceiver(this.m_intentReceiver, new IntentFilter(TAG()));
    }

    protected void onDestroy() {
        this.m_owner.unregisterReceiver(this.m_intentReceiver);
    }

    public boolean SubmitRequest(RequestBase request) {
        request.SetCallerIntent(TAG());
        SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(request);
        return true;
    }

    public static void PresentLoginActivity(Activity owner, LoginAction eLoginAction, LoginChangedListener listener) {
        if (eLoginAction == LoginAction.LOGOUT) {
            SteamWebApi.LogOut();
        }
        if (!SteamWebApi.IsLoggedIn() || eLoginAction == LoginAction.LOGIN_EVEN_IF_LOGGED_IN) {
            Context context;
            Intent intent = new Intent();
            if (owner != null) {
                context = owner;
            } else {
                context = SteamCommunityApplication.GetInstance().getApplicationContext();
            }
            intent.setClass(context, LoginActivity.class);
            intent.setAction("android.intent.action.VIEW");
            intent.setData(LoginActivity.URI_LoginPage);
            intent.putExtra("eLoginAction", eLoginAction.toString());
            LoginActivity.m_callBacksBeforeCreate.add(new WeakReference(listener));
            if (owner != null) {
                owner.startActivityForResult(intent, 0);
            } else {
                SteamCommunityApplication.GetInstance().getApplicationContext().startActivity(intent.addFlags(268435456));
            }
        }
    }

    public static void PresentLoginActivity(Activity owner, LoginAction eLoginAction) {
        PresentLoginActivity(owner, eLoginAction, null);
    }

    static {
        m_commErrorDlg = null;
    }

    public static void DisplayCommunicationErrorMsg(Context context) {
        if (m_commErrorDlg == null) {
            Builder builder = new Builder(context);
            builder.setMessage(R.string.servers_unreachable);
            builder.setIcon(17301543);
            m_commErrorDlg = builder.create();
        }
        if (!m_commErrorDlg.isShowing()) {
            m_commErrorDlg.show();
        }
    }

    public static void UpdateToggleButtonStyles(ToggleButton btn) {
        int residColor = R.color.tab_selector_default;
        int residBgColor = R.color.tab_selector_bg_default;
        int fontStyle = 0;
        if (btn.isChecked()) {
            residColor = R.color.tab_selector_active;
            residBgColor = R.color.tab_selector_bg_active;
            fontStyle = 1;
        }
        btn.setTextColor(SteamCommunityApplication.GetInstance().getResources().getColor(residColor));
        btn.setBackgroundColor(SteamCommunityApplication.GetInstance().getResources().getColor(residBgColor));
        btn.setTypeface(null, fontStyle);
    }

    public static Fragment GetFragmentForActivityById(Activity act, int id) {
        if (!(act instanceof FragmentActivity)) {
            return null;
        }
        FragmentManager fm = ((FragmentActivity) act).getSupportFragmentManager();
        return fm != null ? fm.findFragmentById(id) : null;
    }

    public static TitlebarFragment GetTitlebarFragmentForActivity(Activity act) {
        Fragment frag = GetFragmentForActivityById(act, R.id.titlebar);
        if (frag == null) {
            return null;
        }
        return !(frag instanceof TitlebarFragment) ? null : (TitlebarFragment) frag;
    }

    public static NavigationFragment GetNavigationFragmentForActivity(Activity act) {
        Fragment frag = GetFragmentForActivityById(act, R.id.navigation);
        if (frag == null) {
            return null;
        }
        return !(frag instanceof NavigationFragment) ? null : (NavigationFragment) frag;
    }
}
