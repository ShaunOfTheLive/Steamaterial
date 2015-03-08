package com.valvesoftware.android.steam.community.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SettingInfo;
import com.valvesoftware.android.steam.community.SettingInfo.AccessRight;
import com.valvesoftware.android.steam.community.SettingInfo.CustomDatePickerDialog;
import com.valvesoftware.android.steam.community.SettingInfo.DateConverter;
import com.valvesoftware.android.steam.community.SettingInfo.RadioSelectorItem;
import com.valvesoftware.android.steam.community.SettingInfoDB;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamUriHandler;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamUriHandler.CommandProperty;
import com.valvesoftware.android.steam.community.SteamUriHandler.Result;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.activity.ChatActivity;
import com.valvesoftware.android.steam.community.activity.LoginActivity;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginAction;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginChangedListener;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment.TitlebarButtonHander;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;

public class WebViewFragment extends Fragment {
    private static final int[] m_categoryButtons;
    private SteamWebChromeClient m_chromeViewClient;
    private SteamWebViewProgressDialog m_progress;
    private int m_selectedTab;
    private final OnCheckedChangeListener m_toggleListener;
    private String m_url;
    private WebView m_webView;
    private SteamWebViewClient m_webViewClient;
    private OnTouchListener m_webViewTouchListener;

    public static interface WebViewFragmentOwner {

        public static class URLCategory {
            public String title;
            public String url;
        }

        String GetRootURL();
    }

    class AnonymousClass_4 implements TitlebarButtonHander {
        final /* synthetic */ Uri val$uriSearch;

        AnonymousClass_4(Uri uri) {
            this.val$uriSearch = uri;
        }

        public void onTitlebarButtonClicked(int id) {
            WebViewFragment.this.getActivity().startActivity(new Intent().addFlags(402653184).setClass(WebViewFragment.this.getActivity(), SteamMobileUriActivity.class).setAction("android.intent.action.VIEW").putExtra("title_resid", R.string.Search).setData(this.val$uriSearch));
        }
    }

    static /* synthetic */ class AnonymousClass_5 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command = new int[Command.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.opencategoryurl.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.openurl.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.openexternalurl.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.mobileloginsucceeded.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.reloadpage.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.errorrecovery.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.login.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.closethis.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.notfound.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.agecheckfailed.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.agecheck.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.settitle.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[Command.chat.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    private class SteamWebChromeClient extends WebChromeClient {
        private SteamWebChromeClient() {
        }

        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress > 25) {
                WebViewFragment.this.m_progress.hide();
                WebViewFragment.this.requestWebViewFocusAndEnableTyping();
            }
        }

        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return super.onJsAlert(view, url, message, result);
        }
    }

    private class SteamWebViewClient extends WebViewClient implements LoginChangedListener {
        static final /* synthetic */ boolean $assertionsDisabled;
        private Result m_loginContext;
        private WeakReference<WebView> m_urlWebView;

        class AnonymousClass_2 implements OnDateSetListener {
            final /* synthetic */ OnClickListener val$onPickerButtons;
            final /* synthetic */ SettingInfo val$settingInfo;

            AnonymousClass_2(SettingInfo settingInfo, OnClickListener onClickListener) {
                this.val$settingInfo = settingInfo;
                this.val$onPickerButtons = onClickListener;
            }

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                this.val$settingInfo.setValueAndCommit(WebViewFragment.this.getActivity().getApplicationContext(), DateConverter.makeValue(year, monthOfYear, dayOfMonth));
                this.val$onPickerButtons.onClick(null, WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME);
            }
        }

        class AnonymousClass_3 extends RadioSelectorItemOnClickListener {
            private SslErrorHandler m_hdlrDelayed;
            final /* synthetic */ SslErrorHandler val$hdlrDelayed;

            AnonymousClass_3(Activity x0, SettingInfo x1, TextView x2, SslErrorHandler sslErrorHandler) {
                this.val$hdlrDelayed = sslErrorHandler;
                super(x0, x1, x2);
                this.m_hdlrDelayed = this.val$hdlrDelayed;
            }

            public void onSettingChanged(RadioSelectorItem sel) {
                if (sel.value != 1) {
                    this.m_hdlrDelayed.proceed();
                    return;
                }
                WebViewFragment.this.setCategoriesFailed();
                this.m_hdlrDelayed.cancel();
            }
        }

        static {
            $assertionsDisabled = !WebViewFragment.class.desiredAssertionStatus();
        }

        private SteamWebViewClient() {
        }

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (WebViewFragment.this.getActivity() == null) {
                return true;
            }
            if (url.startsWith(SteamUriHandler.MOBILE_PROTOCOL)) {
                Result result = SteamUriHandler.HandleSteamURI(Uri.parse(url));
                if (result.handled) {
                    Intent resultIntent;
                    switch (AnonymousClass_5.$SwitchMap$com$valvesoftware$android$steam$community$SteamUriHandler$Command[result.command.ordinal()]) {
                        case UriData.RESULT_INVALID_CONTENT:
                        case UriData.RESULT_HTTP_EXCEPTION:
                            WebViewFragment.this.getActivity().startActivityForResult(new Intent().setData(Uri.parse(url)).setAction("android.intent.action.VIEW").setClass(WebViewFragment.this.getActivity(), SteamMobileUriActivity.class), 0);
                            break;
                        case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                            try {
                                WebViewFragment.this.getActivity().startActivity(new Intent().setData(Uri.parse(result.getProperty(CommandProperty.url))).setAction("android.intent.action.VIEW"));
                            } catch (Exception e) {
                            }
                        case AccessibilityNodeInfoCompat.ACTION_SELECT:
                            if (WebViewFragment.this.getActivity() instanceof LoginActivity) {
                                ((LoginActivity) WebViewFragment.this.getActivity()).OnMobileLoginSucceeded(result);
                            }
                            break;
                        case MotionEventCompat.ACTION_POINTER_DOWN:
                            if (WebViewFragment.this.getActivity() instanceof SteamMobileUriActivity) {
                                ((SteamMobileUriActivity) WebViewFragment.this.getActivity()).ReloadPage();
                            }
                            break;
                        case MotionEventCompat.ACTION_POINTER_UP:
                            WebViewFragment.this.requestWebViewFocusAndEnableTyping();
                            view.loadUrl(WebViewFragment.this.m_url);
                            break;
                        case MotionEventCompat.ACTION_HOVER_MOVE:
                            this.m_urlWebView = new WeakReference(view);
                            this.m_loginContext = result;
                            ActivityHelper.PresentLoginActivity(WebViewFragment.this.getActivity(), LoginAction.LOGIN_EVEN_IF_LOGGED_IN, this);
                            break;
                        case AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION:
                            if ($assertionsDisabled || result.getProperty(CommandProperty.call) != null) {
                                resultIntent = WebViewFragment.this.getActivity().getIntent();
                                resultIntent.putExtra(CommandProperty.call.toString(), result.getProperty(CommandProperty.call));
                                resultIntent.setAction(SteamCommunityApplication.INTENT_ACTION_WEBVIEW_RESULT);
                                WebViewFragment.this.getActivity().setResult(-1, resultIntent);
                                WebViewFragment.this.getActivity().finish();
                            } else {
                                throw new AssertionError();
                            }
                        case 9:
                            boolean bFinishThis = true;
                            if ((WebViewFragment.this.getActivity() instanceof SteamMobileUriActivity) && ((SteamMobileUriActivity) WebViewFragment.this.getActivity()).isCategoriesUrl()) {
                                bFinishThis = false;
                                WebViewFragment.this.setViewContentsShowFailure(SteamUriHandler.MOBILE_PROTOCOL + Command.reloadpage.toString(), SteamCommunityApplication.GetInstance().getString(R.string.Web_Error_Reload));
                            }
                            if (bFinishThis) {
                                resultIntent = WebViewFragment.this.getActivity().getIntent();
                                resultIntent.putExtra("dialogtext", WebViewFragment.this.getResources().getString(R.string.notfound));
                                resultIntent.setAction(SteamCommunityApplication.INTENT_ACTION_WEBVIEW_RESULT);
                                WebViewFragment.this.getActivity().setResult(-1, resultIntent);
                                WebViewFragment.this.getActivity().finish();
                            }
                            break;
                        case 10:
                            resultIntent = WebViewFragment.this.getActivity().getIntent();
                            resultIntent.putExtra("dialogtext", WebViewFragment.this.getResources().getString(R.string.agecheckfailed));
                            resultIntent.setAction(SteamCommunityApplication.INTENT_ACTION_WEBVIEW_RESULT);
                            WebViewFragment.this.getActivity().setResult(-1, resultIntent);
                            WebViewFragment.this.getActivity().finish();
                            break;
                        case 11:
                            Calendar myDOB;
                            SettingInfo settingInfo = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingDOB;
                            if (settingInfo == null) {
                                myDOB = Calendar.getInstance();
                            } else {
                                myDOB = DateConverter.makeCalendar(settingInfo.getValue(WebViewFragment.this.getActivity().getApplicationContext()));
                            }
                            OnClickListener onPickerButtons = new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == -2) {
                                        WebViewFragment.this.getActivity().setResult(-1);
                                        WebViewFragment.this.getActivity().finish();
                                    } else if (dialog == null && which == -3) {
                                        WebViewFragment.this.requestWebViewFocusAndEnableTyping();
                                        WebViewFragment.this.m_webView.loadUrl(WebViewFragment.this.m_url);
                                        WebViewFragment.this.m_progress.show();
                                    }
                                }
                            };
                            DatePickerDialog picker = new CustomDatePickerDialog(WebViewFragment.this.getActivity(), new AnonymousClass_2(settingInfo, onPickerButtons), myDOB, 2131165263);
                            picker.setButton(-2, WebViewFragment.this.getText(R.string.Cancel), onPickerButtons);
                            picker.show();
                            break;
                        case 12:
                            String title = result.getProperty(CommandProperty.title);
                            if (title != null) {
                                try {
                                    String titleDecoded = URLDecoder.decode(title);
                                    TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(WebViewFragment.this.getActivity());
                                    if (titlebar != null) {
                                        titlebar.setTitleLabel(titleDecoded);
                                    }
                                } catch (Exception e2) {
                                }
                            }
                        case 13:
                            String steamid = result.getProperty(CommandProperty.steamid);
                            WebViewFragment.this.startActivity(new Intent().addFlags(402653184).setClass(WebViewFragment.this.getActivity(), ChatActivity.class).putExtra(CommandProperty.steamid.toString(), steamid).setAction("android.intent.action.VIEW"));
                            break;
                    }
                }
            } else {
                WebViewFragment.this.requestWebViewFocusAndEnableTyping();
                view.loadUrl(url);
                WebViewFragment.this.m_progress.show();
            }
            return true;
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (WebViewFragment.this.getActivity() != null && WebViewFragment.this.m_progress != null && WebViewFragment.this.m_progress.isShowing()) {
                WebViewFragment.this.getActivity().setTitle(WebViewFragment.this.getResources().getString(R.string.Steam));
                WebViewFragment.this.setViewContentsShowFailure(SteamUriHandler.MOBILE_PROTOCOL + Command.errorrecovery.toString(), description);
            }
        }

        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            SettingInfo settingInfo = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingSslUntrustedPrompt;
            if (settingInfo.m_access == AccessRight.NONE) {
                handler.cancel();
            } else if (settingInfo.getRadioSelectorItemValue(SteamCommunityApplication.GetInstance().getApplicationContext()).value == -1) {
                handler.proceed();
            } else {
                SslErrorHandler hdlrDelayed = handler;
                new AnonymousClass_3(WebViewFragment.this.getActivity(), settingInfo, null, hdlrDelayed).onClick(null);
            }
        }

        public void onLoginChangedSuccessfully() {
            doNavigationInWebView((WebView) this.m_urlWebView.get(), this.m_loginContext);
        }

        private void doNavigationInWebView(WebView view, Result obj) {
            if (view != null) {
                String sCall = obj.getProperty(CommandProperty.call);
                if (sCall == null || sCall.length() <= 0) {
                    String sUrl = obj.getProperty(CommandProperty.url);
                    if (sUrl != null && sUrl.length() > 0) {
                        WebViewFragment.this.requestWebViewFocusAndEnableTyping();
                        view.loadUrl(sUrl);
                        WebViewFragment.this.m_progress.show();
                        return;
                    }
                    return;
                }
                view.loadUrl("javascript:(function(){" + sCall + ";})()");
            }
        }
    }

    private class SteamWebViewProgressDialog extends ProgressDialog {
        public SteamWebViewProgressDialog(Context context) {
            super(context);
        }

        public void onBackPressed() {
            WebViewFragment.this.m_webView.setWebViewClient(null);
            WebViewFragment.this.m_webView.stopLoading();
            super.onBackPressed();
            WebViewFragment.this.getActivity().finish();
        }
    }

    private class ToggleOnClickListener implements View.OnClickListener {
        String m_clickUrl;

        ToggleOnClickListener(String clickUrl) {
            this.m_clickUrl = clickUrl;
        }

        public void onClick(View v) {
            ToggleButton button = (ToggleButton) v;
            RadioGroup group = (RadioGroup) v.getParent();
            boolean wasChecked = group.getCheckedRadioButtonId() == v.getId();
            group.check(v.getId());
            WebViewFragment.this.m_url = this.m_clickUrl;
            if (wasChecked) {
                button.setChecked(true);
            } else {
                WebViewFragment.this.requestWebViewFocusAndEnableTyping();
                WebViewFragment.this.m_webView.loadUrl(WebViewFragment.this.m_url);
                WebViewFragment.this.m_progress.show();
            }
            WebViewFragment.this.UpdateToggleButtonStyles();
        }
    }

    public WebViewFragment() {
        this.m_webView = null;
        this.m_url = null;
        this.m_webViewClient = new SteamWebViewClient();
        this.m_chromeViewClient = new SteamWebChromeClient();
        this.m_selectedTab = 0;
        this.m_toggleListener = new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                for (int j = 0; j < radioGroup.getChildCount(); j++) {
                    ToggleButton view = (ToggleButton) radioGroup.getChildAt(j);
                    view.setChecked(view.getId() == i);
                    if (view.getId() == i) {
                        WebViewFragment.this.m_selectedTab = j;
                    }
                }
            }
        };
        this.m_webViewTouchListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case SettingInfoDB.SETTING_NOTIFY_FIRST:
                    case UriData.RESULT_INVALID_CONTENT:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        };
    }

    private String GetDebugName() {
        return getClass().getSimpleName() + "[" + (this.m_url != null ? this.m_url : "") + "]@" + (this.m_webView != null ? this.m_webView.getUrl() : "");
    }

    private void requestWebViewFocusAndEnableTyping() {
        if (!this.m_webView.hasFocus()) {
            this.m_webView.requestFocus();
        }
        this.m_webView.setOnTouchListener(this.m_webViewTouchListener);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.m_progress = new SteamWebViewProgressDialog(getActivity());
        this.m_progress.setMessage(getResources().getString(R.string.Loading));
        this.m_progress.setIndeterminate(true);
        this.m_progress.setCancelable(true);
        if (getActivity() != null && (getActivity() instanceof WebViewFragmentOwner)) {
            WebViewFragmentOwner fragmentOwner = (WebViewFragmentOwner) getActivity();
            if (fragmentOwner != null) {
                this.m_url = fragmentOwner.GetRootURL();
            }
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.webview_fragment, container, false);
    }

    public void onResume() {
        super.onResume();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.m_webView == null) {
            this.m_webView = (WebView) getActivity().findViewById(R.id.webView);
            this.m_webView.setBackgroundColor(0);
            this.m_webView.setWebViewClient(this.m_webViewClient);
            this.m_webView.setWebChromeClient(this.m_chromeViewClient);
            this.m_webView.getSettings().setJavaScriptEnabled(true);
            this.m_webView.getSettings().setDomStorageEnabled(true);
            this.m_webView.setScrollBarStyle(0);
            this.m_webView.setHorizontalScrollBarEnabled(false);
            if (!(this.m_webView == null || this.m_url == null)) {
                requestWebViewFocusAndEnableTyping();
                this.m_webView.loadUrl(this.m_url);
                this.m_progress.show();
            }
            ((RadioGroup) getActivity().findViewById(R.id.toggleGroup)).setOnCheckedChangeListener(this.m_toggleListener);
        }
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setRefreshHandler(new TitlebarButtonHander() {
                public void onTitlebarButtonClicked(int id) {
                    if (WebViewFragment.this.getActivity() instanceof SteamMobileUriActivity) {
                        ((SteamMobileUriActivity) WebViewFragment.this.getActivity()).ReloadPage();
                    }
                }
            });
        }
    }

    public void onDestroy() {
        super.onDestroy();
        this.m_progress.dismiss();
        this.m_webView.stopLoading();
    }

    public void loadUrl(String sUrl) {
        requestWebViewFocusAndEnableTyping();
        this.m_webView.loadUrl(sUrl);
        if (!sUrl.startsWith("javascript:")) {
            this.m_progress.show();
        }
    }

    static {
        m_categoryButtons = new int[]{2131296317, 2131296318, 2131296319, 2131296320, 2131296321, 2131296322, 2131296323, 2131296324};
    }

    private void UpdateToggleButtonStyles() {
        for (int i = 0; i < m_categoryButtons.length; i++) {
            ToggleButton catButton = (ToggleButton) getActivity().findViewById(m_categoryButtons[i]);
            if (catButton != null) {
                ActivityHelper.UpdateToggleButtonStyles(catButton);
            }
        }
    }

    public void setCategories(ArrayList<URLCategory> categoryList, String searchURL) {
        if (categoryList != null && categoryList.size() > 0) {
            int i;
            if (this.m_selectedTab > categoryList.size() - 1) {
                this.m_selectedTab = categoryList.size() - 1;
            }
            LinearLayout btnLayout = (LinearLayout) getActivity().findViewById(R.id.web_select_layout);
            btnLayout.setWeightSum((float) categoryList.size());
            if (categoryList.size() > 1) {
                i = 0;
            } else {
                i = 8;
            }
            btnLayout.setVisibility(i);
            int i2 = 0;
            while (i2 < categoryList.size()) {
                boolean z;
                ToggleButton catButton = (ToggleButton) getActivity().findViewById(m_categoryButtons[i2]);
                catButton.setVisibility(0);
                catButton.setText(((URLCategory) categoryList.get(i2)).title);
                catButton.setTextOn(((URLCategory) categoryList.get(i2)).title);
                catButton.setTextOff(((URLCategory) categoryList.get(i2)).title);
                catButton.setOnClickListener(new ToggleOnClickListener(((URLCategory) categoryList.get(i2)).url));
                if (i2 == this.m_selectedTab) {
                    z = true;
                } else {
                    z = false;
                }
                catButton.setChecked(z);
                i2++;
            }
            while (i2 < m_categoryButtons.length) {
                ((ToggleButton) getActivity().findViewById(m_categoryButtons[i2])).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                i2++;
            }
            UpdateToggleButtonStyles();
            this.m_url = ((URLCategory) categoryList.get(this.m_selectedTab)).url;
            loadUrl(this.m_url);
        }
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar == null) {
            return;
        }
        if (searchURL == null || searchURL.equals("")) {
            titlebar.setSearchHandler(null);
        } else {
            titlebar.setSearchHandler(new AnonymousClass_4(Uri.parse(searchURL)));
        }
    }

    public void setCategoriesFailed() {
        setViewContentsShowFailure(SteamUriHandler.MOBILE_PROTOCOL + Command.reloadpage.toString(), SteamCommunityApplication.GetInstance().getString(R.string.Web_Error_Reload));
    }

    public void setViewContentsShowFailure(String hrefRetry, String description) {
        this.m_webView.loadData("<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body bgcolor=\"#181818\" text=\"#D6D6D6\" link=\"#FFFFFF\" alink=\"#FFFFFF\" vlink=\"#FFFFFF\"><br/><h2 align=\"center\">" + SteamCommunityApplication.GetInstance().getString(R.string.Web_Error_Title) + "</h2>" + "<p align=\"left\">" + description + "</p>" + "<p align=\"center\">" + "<a href=\"" + hrefRetry + "\">" + SteamCommunityApplication.GetInstance().getString(R.string.Web_Error_Retry_Now) + "</a></p>" + "</body></html>", "text/html", "utf-8");
    }
}
