package com.valvesoftware.android.steam.community.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.valvesoftware.android.steam.community.Config;
import com.valvesoftware.android.steam.community.Config.WebAPI;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamUriHandler.CommandProperty;
import com.valvesoftware.android.steam.community.SteamUriHandler.Result;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.SteamWebApi.LoginInformation;
import com.valvesoftware.android.steam.community.SteamWebApi.LoginState;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestForLogin;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LoginActivity extends SteamMobileUriActivity {
    public static final Uri URI_LoginPage;
    public static ArrayList<WeakReference<LoginChangedListener>> m_callBacksBeforeCreate;
    private ArrayList<WeakReference<LoginChangedListener>> m_callBacks;

    public enum LoginAction {
        LOGOUT,
        LOGIN_DEFAULT,
        LOGIN_EVEN_IF_LOGGED_IN
    }

    public static interface LoginChangedListener {
        void onLoginChangedSuccessfully();
    }

    public LoginActivity() {
        this.m_callBacks = new ArrayList();
    }

    static {
        URI_LoginPage = Uri.parse("steammobile://openurl?url=" + Config.URL_COMMUNITY_BASE + "/mobilelogin" + "?oauth_client_id=" + WebAPI.OAUTH_CLIENT_ID + "&oauth_scope=read_profile%20write_profile%20read_client%20write_client");
        m_callBacksBeforeCreate = new ArrayList();
    }

    public void onCreate(Bundle savedInstanceState) {
        this.m_residActivityLayout = 2130903052;
        super.onCreate(savedInstanceState);
        this.m_callBacks.addAll(m_callBacksBeforeCreate);
        m_callBacksBeforeCreate.clear();
    }

    public void onBackPressed() {
        if (FragmentActivityWithNavigationSupport.hasValidActivities()) {
            super.onBackPressed();
        } else {
            finish();
        }
    }

    public void finish() {
        super.finish();
        if (!FragmentActivityWithNavigationSupport.hasValidActivities() && SteamWebApi.IsLoggedIn()) {
            SteamCommunityApplication.GetInstance().startActivity(new Intent().setClass(SteamCommunityApplication.GetInstance(), CommunityActivity.class).setFlags(805306368));
        }
    }

    private void dispatchOnLoginChangedSuccessfully() {
        ArrayList<WeakReference<LoginChangedListener>> callBacksCopy = this.m_callBacks;
        this.m_callBacks = new ArrayList();
        Iterator i$ = callBacksCopy.iterator();
        while (i$.hasNext()) {
            LoginChangedListener listener = (LoginChangedListener) ((WeakReference) i$.next()).get();
            if (listener != null) {
                listener.onLoginChangedSuccessfully();
            }
        }
    }

    private boolean HandleLoginDocument(JSONObject doc) throws JSONException {
        if (!doc.has("access_token") || !doc.has("x_steamid")) {
            return false;
        }
        if (SteamWebApi.IsLoggedIn()) {
            SteamWebApi.LogOut();
        }
        SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite().Write(RequestForLogin.CACHE_FILENAME, doc.toString().getBytes());
        LoginInformation loginInformation = RequestForLogin.GetLoginInformation();
        loginInformation.m_loginState = LoginState.LoggedIn;
        loginInformation.m_accessToken = doc.getString("access_token");
        loginInformation.m_steamID = doc.getString("x_steamid");
        SteamWebApi.SetLoginInformation(loginInformation);
        setResult(-1);
        dispatchOnLoginChangedSuccessfully();
        finish();
        return true;
    }

    public void OnMobileLoginSucceeded(Result result) {
        try {
            JSONObject doc = new JSONObject();
            doc.put("x_steamid", result.getProperty(CommandProperty.steamid));
            doc.put("access_token", result.getProperty(CommandProperty.oauth_token));
            doc.put("x_webcookie", result.getProperty(CommandProperty.webcookie));
            boolean bSuccess = HandleLoginDocument(doc);
        } catch (Exception e) {
            bSuccess = false;
        }
        if (!bSuccess) {
            ReloadPage();
        }
    }

    public static void PreemptivelyLoginBasedOnCachedLoginDocument() {
        byte[] logindata = SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite().Read(RequestForLogin.CACHE_FILENAME);
        if (logindata != null) {
            LoginInformation loginInformationFinal = null;
            try {
                Object doc = new JSONTokener(new String(logindata)).nextValue();
                if (JSONObject.class.isInstance(doc)) {
                    JSONObject jsonDoc = (JSONObject) doc;
                    if (jsonDoc.has("access_token") && jsonDoc.has("x_steamid")) {
                        LoginInformation loginInformation = new LoginInformation();
                        loginInformation.m_loginState = LoginState.LoggedIn;
                        loginInformation.m_accessToken = jsonDoc.getString("access_token");
                        loginInformation.m_steamID = jsonDoc.getString("x_steamid");
                        loginInformationFinal = loginInformation;
                    }
                }
            } catch (JSONException e) {
            }
            if (loginInformationFinal != null) {
                SteamWebApi.SetLoginInformation(loginInformationFinal);
            }
        }
    }
}
