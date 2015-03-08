package com.valvesoftware.android.steam.community.activity;

import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamDBResponseListener;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamUriHandler;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamUriHandler.CommandProperty;
import com.valvesoftware.android.steam.community.SteamUriHandler.Result;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestDocumentType;
import com.valvesoftware.android.steam.community.fragment.WebViewFragment;
import com.valvesoftware.android.steam.community.fragment.WebViewFragment.WebViewFragmentOwner;
import com.valvesoftware.android.steam.community.fragment.WebViewFragment.WebViewFragmentOwner.URLCategory;
import java.util.ArrayList;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SteamMobileUriActivity extends FragmentActivityWithNavigationSupport implements WebViewFragmentOwner, SteamDBResponseListener {
    private static int s_NextActivityID;
    private String m_CategoriesUrl;
    private ActivityHelper m_activityHelper;
    private ArrayList<URLCategory> m_categoryList;
    private CategoryRequest m_reqTOC;
    protected int m_residActivityLayout;
    private String m_url;

    private static class CategoryRequest extends RequestBase {
        public CategoryRequest(String jobqueue) {
            super(jobqueue);
        }

        protected HttpRequestBase GetDefaultHttpRequestBase() throws Exception {
            HttpRequestBase req = super.GetDefaultHttpRequestBase();
            String cookieString = SteamWebApi.GetCookieHeaderString(false);
            if (cookieString != null) {
                req.addHeader("Cookie", cookieString);
            }
            return req;
        }
    }

    public SteamMobileUriActivity() {
        this.m_url = null;
        this.m_CategoriesUrl = null;
        this.m_categoryList = null;
        this.m_activityHelper = new ActivityHelper(this);
        this.m_residActivityLayout = 2130903064;
        this.m_reqTOC = null;
    }

    static {
        s_NextActivityID = 0;
    }

    public static String GetNextActivityID() {
        StringBuilder append = new StringBuilder().append("SteamMobileUriActivity_");
        int i = s_NextActivityID;
        s_NextActivityID = i + 1;
        return append.append(i).toString();
    }

    public boolean isCategoriesUrl() {
        return this.m_CategoriesUrl != null;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.m_activityHelper.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Result result = null;
        if ("android.intent.action.VIEW".equals(intent.getAction())) {
            result = SteamUriHandler.HandleSteamURI(intent.getData());
            if (result.command == Command.opencategoryurl) {
                this.m_categoryList = new ArrayList();
                this.m_CategoriesUrl = result.getProperty(CommandProperty.url);
            } else if (result.command == Command.openurl) {
                this.m_url = result.getProperty(CommandProperty.url);
            } else if (result.command == Command.chat) {
                String steamid = result.getProperty(CommandProperty.steamid);
                Intent chatIntent = new Intent().setClass(this, ChatActivity.class).addFlags(402653184);
                chatIntent.putExtra(CommandProperty.steamid.toString(), steamid);
                chatIntent.setAction("android.intent.action.VIEW");
                startActivity(chatIntent);
                finish();
            }
        }
        if (result == null || !result.handled) {
            finish();
            return;
        }
        if (Command.opencategoryurl == result.command) {
            PerformCategoryUrlWebApiRequest();
        }
        setContentView(this.m_residActivityLayout);
    }

    private void PerformCategoryUrlWebApiRequest() {
        this.m_reqTOC = new CategoryRequest(SteamDBService.JOB_QUEUE_CATALOG);
        this.m_reqTOC.SetUriAndDocumentType(this.m_CategoriesUrl, RequestDocumentType.JSON);
        this.m_reqTOC.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
        this.m_activityHelper.SubmitRequest(this.m_reqTOC);
    }

    protected void onDestroy() {
        super.onDestroy();
        this.m_activityHelper.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (result != null) {
            String sCall = result.getStringExtra(CommandProperty.call.toString());
            if (sCall == null || sCall.length() <= 0) {
                String sUrl = result.getStringExtra(CommandProperty.url.toString());
                if (sUrl == null || sUrl.length() <= 0) {
                    String sDlgText = result.getStringExtra("dialogtext");
                    if (sDlgText != null && sDlgText.length() > 0) {
                        new Builder(this).setMessage(sDlgText).show();
                        return;
                    }
                    return;
                }
                ((WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webview)).loadUrl(sUrl);
                return;
            }
            ((WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webview)).loadUrl("javascript:(function(){" + sCall + ";})()");
        }
    }

    public void ReloadPage() {
        Intent intent = getIntent();
        if ("android.intent.action.VIEW".equals(intent.getAction())) {
            Result result = SteamUriHandler.HandleSteamURI(intent.getData());
            if (result.command == Command.opencategoryurl) {
                PerformCategoryUrlWebApiRequest();
            } else if (result.command == Command.openurl) {
                ((WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webview)).loadUrl(this.m_url);
            }
        }
    }

    public String GetRootURL() {
        return this.m_url;
    }

    public void OnRequestCompleted(Integer intentId) {
        if (this.m_reqTOC != null && this.m_reqTOC.GetIntentId() == intentId.intValue()) {
            if (this.m_reqTOC.GetJSONDocument() != null) {
                try {
                    HandleTOCDocument();
                } catch (JSONException e) {
                    HandleTOCDocumentFailure();
                }
            } else {
                HandleTOCDocumentFailure();
            }
            this.m_reqTOC = null;
        }
    }

    private void HandleTOCDocumentFailure() {
        ((WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webview)).setCategoriesFailed();
    }

    private void HandleTOCDocument() throws JSONException {
        JSONObject doc = this.m_reqTOC.GetJSONDocument();
        if (doc.getBoolean("success")) {
            int jj;
            this.m_categoryList.clear();
            JSONArray arrMsgs = doc.getJSONArray("categories");
            int len = arrMsgs != null ? arrMsgs.length() : 0;
            for (jj = 0; jj < len; jj++) {
                JSONObject jcat = arrMsgs.getJSONObject(jj);
                URLCategory cat = new URLCategory();
                cat.title = jcat.getString("label");
                cat.url = jcat.getString("url");
                this.m_categoryList.add(cat);
            }
            String searchUri = null;
            try {
                JSONArray arrOptions = doc.getJSONArray("options");
                for (jj = 0; jj < len; jj++) {
                    JSONObject jopt = arrOptions.getJSONObject(jj);
                    String lbl = jopt.optString("label");
                    if (lbl != null && lbl.equals("search")) {
                        searchUri = jopt.optString("url");
                    }
                }
            } catch (Exception e) {
            }
            WebViewFragment fragment = (WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webview);
            if (fragment != null) {
                fragment.setCategories(this.m_categoryList, searchUri);
            }
        }
    }
}
