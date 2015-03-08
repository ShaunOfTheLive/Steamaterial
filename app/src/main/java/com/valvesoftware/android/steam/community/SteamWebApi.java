package com.valvesoftware.android.steam.community;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.valvesoftware.android.steam.community.Config.SteamUniverse;
import com.valvesoftware.android.steam.community.Config.WebAPI;
import com.valvesoftware.android.steam.community.SettingInfo.DateConverter;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_LOGININFO_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamWebApi.DevUniverseSSLSocketFactory;
import com.valvesoftware.android.steam.community.SteamWebApi.LoginInformation;
import com.valvesoftware.android.steam.community.SteamWebApi.LoginState;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestDocumentType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestHttpMode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SteamWebApi {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final String RequestForAppSummariesByAppIDs_URI;
    private static final String RequestForFriendListByOwnerSteamID_URI;
    private static final String RequestForGroupListByOwnerSteamID_URI;
    private static final String RequestForGroupsBySteamIDs_URI;
    private static final String RequestForUserSummariesBySteamIDs_URI;
    private static final String TAG = "SteamWebApi";
    private static boolean s_bCookiesAreOutOfDate;
    private static Map<String, Map<String, String>> s_cookiesConfiguration;
    private static LoginInformation s_loginInformation;

    public static class RequestBase {
        static final /* synthetic */ boolean $assertionsDisabled;
        private static final String HTTP_PARAM_DEFAULT_PROXY = "proxy.valvesoftware.com";
        private static final int HTTP_PARAM_DEFAULT_PROXY_PORT = 3128;
        private static final boolean HTTP_PARAM_DEFAULT_USING_PROXY = false;
        private static final int HTTP_TIMEOUT_DEFAULT_CONN = 25000;
        private static final int HTTP_TIMEOUT_DEFAULT_SO = 25000;
        private DiskCacheInfo m_DiskCacheInfo;
        protected Bitmap m_bitmap;
        private String m_callerIntent;
        private String m_callerRequestQueue;
        protected JSONObject m_jsonDocument;
        private Object m_objData;
        private String m_postData;
        private RequestDocumentType m_requestDocumentType;
        private RequestHttpMode m_requestMode;
        private UriData m_result;
        private RequestActionType m_skipMode;
        private String m_uri;

        public static class DiskCacheInfo {
            SteamDBDiskCache disk;
            String uri;
        }

        static {
            $assertionsDisabled = !SteamWebApi.class.desiredAssertionStatus() ? true : HTTP_PARAM_DEFAULT_USING_PROXY;
        }

        public RequestBase(String requestQueue) {
            this.m_callerIntent = null;
            this.m_callerRequestQueue = null;
            this.m_uri = null;
            this.m_requestMode = RequestHttpMode.Get;
            this.m_postData = null;
            this.m_objData = null;
            this.m_requestDocumentType = RequestDocumentType.String;
            this.m_skipMode = RequestActionType.DoHttpRequestAndCacheResults;
            this.m_result = null;
            this.m_jsonDocument = null;
            this.m_bitmap = null;
            this.m_callerRequestQueue = requestQueue;
        }

        public boolean OnTaskReadyToRunOnJobQueue() {
            return true;
        }

        public String GetRequestQueue() {
            return this.m_callerRequestQueue;
        }

        public int GetIntentId() {
            return System.identityHashCode(this);
        }

        public String GetCallerIntent() {
            return this.m_callerIntent;
        }

        public void SetCallerIntent(String callerIntent) {
            if ($assertionsDisabled || this.m_callerIntent == null) {
                this.m_callerIntent = callerIntent;
                return;
            }
            throw new AssertionError();
        }

        public String GetUri() {
            if ($assertionsDisabled || this.m_uri != null) {
                return this.m_uri;
            }
            throw new AssertionError();
        }

        public String GetUri(int defaultPort) {
            if ($assertionsDisabled || this.m_uri != null) {
                return this.m_uri;
            }
            throw new AssertionError();
        }

        public UriData GetHttpResponseUriData() {
            return this.m_result;
        }

        protected HttpParams GetDefaultHttpParams() {
            BasicHttpParams httpParamsDefault = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParamsDefault, HTTP_TIMEOUT_DEFAULT_SO);
            HttpConnectionParams.setSoTimeout(httpParamsDefault, HTTP_TIMEOUT_DEFAULT_SO);
            return httpParamsDefault;
        }

        protected DefaultHttpClient GetDefaultHttpClient() {
            if (Config.STEAM_UNIVERSE_WEBAPI == SteamUniverse.Dev) {
                try {
                    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    trustStore.load(null, null);
                    SSLSocketFactory sf = new DevUniverseSSLSocketFactory(trustStore);
                    sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                    SchemeRegistry registry = new SchemeRegistry();
                    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                    registry.register(new Scheme("https", sf, 443));
                    HttpParams httpParams = GetDefaultHttpParams();
                    return new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, registry), httpParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new DefaultHttpClient(GetDefaultHttpParams());
        }

        protected HttpRequestBase GetDefaultHttpRequestBase() throws Exception {
            HttpRequestBase httpRequest;
            if (GetMode() == RequestHttpMode.Get) {
                httpRequest = new HttpGet(GetUri(0));
            } else {
                HttpRequestBase httpPost = new HttpPost(GetUri(0));
                httpRequest = httpPost;
                httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setEntity(new StringEntity(GetPostData()));
            }
            httpRequest.addHeader("User-Agent", "Steam App / Android / " + Config.APP_VERSION + " / " + Config.APP_VERSION_ID);
            return httpRequest;
        }

        public HttpResponse FetchHttpResponseOnWorkerThread() throws Exception {
            return GetDefaultHttpClient().execute(GetDefaultHttpRequestBase());
        }

        public boolean IsRequestLive() {
            return this.m_result == null ? HTTP_PARAM_DEFAULT_USING_PROXY : this.m_result.IsRequestLive();
        }

        public String GetDocument() {
            String doc = null;
            if (this.m_result != null) {
                doc = this.m_result.GetDocument();
            }
            return doc == null ? "" : doc;
        }

        public JSONObject GetJSONDocument() {
            return this.m_jsonDocument;
        }

        public Bitmap GetBitmap() {
            return this.m_bitmap;
        }

        public RequestDocumentType GetRequestDocumentType() {
            return this.m_requestDocumentType;
        }

        public void SetUriAndDocumentType(String uri, RequestDocumentType requestDocumentType) {
            if ($assertionsDisabled || this.m_uri == null) {
                this.m_uri = uri;
                this.m_requestDocumentType = requestDocumentType;
                return;
            }
            throw new AssertionError();
        }

        public String GetPostData() {
            return this.m_postData;
        }

        public void SetPostData(String postData) {
            if ($assertionsDisabled || this.m_postData == null) {
                this.m_requestMode = RequestHttpMode.Post;
                this.m_postData = postData;
                return;
            }
            throw new AssertionError();
        }

        public Object GetObjData() {
            return this.m_objData;
        }

        public void SetObjData(Object objData) {
            if ($assertionsDisabled || this.m_objData == null) {
                this.m_objData = objData;
                return;
            }
            throw new AssertionError();
        }

        public RequestHttpMode GetMode() {
            return this.m_requestMode;
        }

        public DiskCacheInfo GetDiskCacheInfo() {
            if (this.m_DiskCacheInfo == null) {
                this.m_DiskCacheInfo = new DiskCacheInfo();
                this.m_DiskCacheInfo.disk = SteamCommunityApplication.GetInstance().GetDiskCacheWeb();
                this.m_DiskCacheInfo.uri = GetUri(0);
            }
            return this.m_DiskCacheInfo;
        }

        public void SetIndefiniteCacheFilename(String filename) {
            if ($assertionsDisabled || this.m_DiskCacheInfo == null) {
                this.m_DiskCacheInfo = new DiskCacheInfo();
                this.m_DiskCacheInfo.disk = SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite();
                this.m_DiskCacheInfo.uri = filename;
                return;
            }
            throw new AssertionError();
        }

        public RequestActionType GetRequestAction() {
            return this.m_skipMode;
        }

        public void SetRequestAction(RequestActionType skipMode) {
            this.m_skipMode = skipMode;
        }

        public boolean IsRequestForByteData() {
            return GetRequestDocumentType() == RequestDocumentType.Bitmap ? true : HTTP_PARAM_DEFAULT_USING_PROXY;
        }

        public void OnResponseWorkerThread(UriData results) throws Exception {
            this.m_result = results;
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestDocumentType[this.m_requestDocumentType.ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    RequestSucceededOnResponseWorkerThread();
                case UriData.RESULT_HTTP_EXCEPTION:
                    try {
                        Object result = new JSONTokener(this.m_result.GetDocument()).nextValue();
                        if (JSONObject.class.isInstance(result)) {
                            this.m_jsonDocument = (JSONObject) result;
                            RequestSucceededOnResponseWorkerThread();
                            return;
                        }
                        this.m_result.SetRequestError(1);
                        OnFailureWorkerThread(results);
                    } catch (JSONException e) {
                        this.m_result.SetRequestError(1);
                        OnFailureWorkerThread(results);
                    }
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    byte[] bmpByteData = this.m_result.GetByteData();
                    this.m_bitmap = bmpByteData != null ? BitmapFactory.decodeByteArray(bmpByteData, 0, bmpByteData.length) : null;
                    if (this.m_bitmap != null) {
                        RequestSucceededOnResponseWorkerThread();
                        return;
                    }
                    this.m_result.SetRequestError(1);
                    OnFailureWorkerThread(results);
                default:
                    OnFailureWorkerThread(results);
            }
        }

        public void OnFailureWorkerThread(UriData results) {
            this.m_result = results;
            RequestFailedOnResponseWorkerThread();
            if (results.m_httpResult == 401) {
                SteamWebApi.LogOut();
            }
        }

        public void RequestSucceededOnResponseWorkerThread() {
        }

        public void RequestFailedOnResponseWorkerThread() {
        }
    }

    static /* synthetic */ class AnonymousClass_1 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestDocumentType;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestDocumentType = new int[RequestDocumentType.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestDocumentType[RequestDocumentType.String.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestDocumentType[RequestDocumentType.JSON.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestDocumentType[RequestDocumentType.Bitmap.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static class DevUniverseSSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext;

        public DevUniverseSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);
            this.sslContext = SSLContext.getInstance("TLS");
            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            this.sslContext.init(null, new TrustManager[]{tm}, null);
        }

        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return this.sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        public Socket createSocket() throws IOException {
            return this.sslContext.getSocketFactory().createSocket();
        }
    }

    public static class LoginInformation {
        public String m_accessToken;
        public LoginState m_loginState;
        public String m_steamID;

        public LoginInformation() {
            LogOut();
        }

        void LogOut() {
            this.m_loginState = LoginState.RequireUsernameAndPassword;
            this.m_steamID = null;
            this.m_accessToken = null;
        }
    }

    public enum LoginState {
        RequireUsernameAndPassword,
        RequireSteamGuardEmailToken,
        LoggedIn
    }

    public enum RequestActionType {
        DoHttpRequestAndCacheResults,
        GetFromCacheOnly,
        GetFromCacheOrDoHttpRequestAndCacheResults,
        DoHttpRequestNoCache
    }

    public enum RequestDocumentType {
        String,
        JSON,
        Bitmap
    }

    public static class RequestForAppSummariesByAppIDs extends RequestBase {
        public String[] m_appIDs;

        public RequestForAppSummariesByAppIDs() {
            super(SteamDBService.JOB_QUEUE_APPS_DB);
            this.m_appIDs = null;
            SetRequestAction(RequestActionType.DoHttpRequestNoCache);
        }
    }

    public static class RequestForFriendListByOwnerSteamID extends RequestBase {
        public String m_steamID;

        public RequestForFriendListByOwnerSteamID() {
            super(SteamDBService.JOB_QUEUE_FRIENDS_DB);
            this.m_steamID = null;
        }
    }

    public static class RequestForGroupListByOwnerSteamID extends RequestBase {
        public String m_steamID;

        public RequestForGroupListByOwnerSteamID() {
            super(SteamDBService.JOB_QUEUE_GROUPS_DB);
            this.m_steamID = null;
        }
    }

    public static class RequestForGroupsBySteamIDs extends RequestBase {
        public String[] m_steamIDs;

        public RequestForGroupsBySteamIDs() {
            super(SteamDBService.JOB_QUEUE_GROUPS_DB);
            this.m_steamIDs = null;
            SetRequestAction(RequestActionType.DoHttpRequestNoCache);
        }
    }

    public static class RequestForLogin {
        public static final String CACHE_FILENAME = "login.json";

        public static LoginInformation GetLoginInformation() {
            return s_loginInformation;
        }
    }

    public static class RequestForUserSummariesBySteamIDs extends RequestBase {
        public String[] m_steamIDs;

        public RequestForUserSummariesBySteamIDs() {
            super(SteamDBService.JOB_QUEUE_FRIENDS_DB);
            this.m_steamIDs = null;
            SetRequestAction(RequestActionType.DoHttpRequestNoCache);
        }
    }

    public enum RequestHttpMode {
        Get,
        Post
    }

    static {
        boolean z;
        if (SteamWebApi.class.desiredAssertionStatus()) {
            z = false;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
        s_loginInformation = new LoginInformation();
        RequestForFriendListByOwnerSteamID_URI = Config.URL_WEBAPI_BASE + "/ISteamUserOAuth/GetFriendList/v0001";
        RequestForUserSummariesBySteamIDs_URI = Config.URL_WEBAPI_BASE + "/ISteamUserOAuth/GetUserSummaries/v0001";
        RequestForGroupListByOwnerSteamID_URI = Config.URL_WEBAPI_BASE + "/ISteamUserOAuth/GetGroupList/v0001";
        RequestForGroupsBySteamIDs_URI = Config.URL_WEBAPI_BASE + "/ISteamUserOAuth/GetGroupSummaries/v0001";
        RequestForAppSummariesByAppIDs_URI = Config.URL_WEBAPI_BASE + "/ISteamGameOAuth/GetAppInfo/v0001";
        s_cookiesConfiguration = new HashMap();
        s_bCookiesAreOutOfDate = false;
    }

    public static LoginState GetLoginState() {
        return s_loginInformation.m_loginState;
    }

    public static boolean IsLoggedIn() {
        return s_loginInformation.m_loginState == LoginState.LoggedIn ? true : $assertionsDisabled;
    }

    public static String GetLoginSteamID() {
        return s_loginInformation.m_steamID;
    }

    public static String GetLoginAccessToken() {
        return s_loginInformation.m_accessToken;
    }

    public static void LogOut() {
        SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite().Delete(RequestForLogin.CACHE_FILENAME);
        s_loginInformation.LogOut();
        SetLoginInformation(s_loginInformation);
        SteamCommunityApplication.GetInstance().GetFriendInfoDB().ClearItems();
        SteamCommunityApplication.GetInstance().GetGroupInfoDB().ClearItems();
    }

    public static void SetLoginInformation(LoginInformation loginInfo) {
        String sOAuthCookie;
        s_loginInformation = loginInfo;
        ResetAllCookies();
        if (s_loginInformation.m_accessToken != null) {
            sOAuthCookie = s_loginInformation.m_steamID + "||oauth:" + s_loginInformation.m_accessToken;
        } else {
            sOAuthCookie = null;
        }
        SetCookie2("steamLogin", sOAuthCookie);
        SetCookie2("steamid", s_loginInformation.m_steamID);
        SettingInfo settingInfo = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingDOB;
        String sDOBvalue = settingInfo == null ? null : settingInfo.getValue(SteamCommunityApplication.GetInstance().getApplicationContext());
        String sDOBcookie = (sDOBvalue == null || sDOBvalue.equals("")) ? null : DateConverter.makeUnixTime(sDOBvalue);
        SetCookie2("dob", sDOBcookie);
        SyncAllCookies();
        REQ_ACT_LOGININFO_DATA obj = new REQ_ACT_LOGININFO_DATA();
        obj.sOAuthToken = s_loginInformation.m_accessToken;
        obj.sSteamID = s_loginInformation.m_steamID;
        SubmitSimpleActionRequest(SteamDBService.REQ_ACT_LOGININFO, obj);
        if (IsLoggedIn()) {
            SteamCommunityApplication.GetInstance().GetFriendInfoDB().RefreshFromCacheAndHttp();
            SteamCommunityApplication.GetInstance().GetGroupInfoDB().RefreshFromCacheAndHttp();
        }
    }

    private static void StartWithAccessToken(StringBuilder sb) {
        sb.append("?access_token=");
        if (s_loginInformation.m_accessToken != null) {
            sb.append(s_loginInformation.m_accessToken);
        } else {
            sb.append(C2DMReceiverService.EXTRA_ERROR);
        }
    }

    private static int PrecomputeAccessTokenLength() {
        return (s_loginInformation.m_accessToken != null ? s_loginInformation.m_accessToken.length() : 16) + 32;
    }

    private static int PrecomputeKeyLength() {
        return 32;
    }

    private static int PrecomputeIdsLength(String[] ids) {
        return ids.length * 22;
    }

    private static void AddIds(StringBuilder sb, String[] ids) {
        if (ids.length > 0) {
            sb.append(ids[0]);
        }
        int length = Math.min(ids.length, WebAPI.MAX_IDS_PER_CALL);
        if ($assertionsDisabled || length == ids.length) {
            int i = 1;
            while (i < length && ids[i] != null) {
                sb.append(",");
                sb.append(ids[i]);
                i++;
            }
            return;
        }
        throw new AssertionError();
    }

    private static void AddSteamIds(StringBuilder sb, String[] steamIDs) {
        sb.append("&steamids=");
        AddIds(sb, steamIDs);
    }

    private static void AddAppIds(StringBuilder sb, String[] appIDs) {
        sb.append("&appids=");
        AddIds(sb, appIDs);
    }

    private static void AddSteamId(StringBuilder sb, String steamID) {
        sb.append("&steamid=");
        sb.append(steamID);
    }

    public static void SubmitSimpleActionRequest(String reqname, Object obj) {
        RequestBase request = new RequestBase(SteamDBService.JOB_QUEUE_SIMPLEACTION);
        request.m_uri = reqname;
        request.m_objData = obj;
        SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(request);
    }

    public static RequestForFriendListByOwnerSteamID GetRequestForFriendListByOwnerSteamID(String steamID, boolean cached) {
        if ($assertionsDisabled || IsLoggedIn()) {
            StringBuilder sb = new StringBuilder(((RequestForFriendListByOwnerSteamID_URI.length() + PrecomputeAccessTokenLength()) + PrecomputeKeyLength()) + steamID.length());
            sb.append(RequestForFriendListByOwnerSteamID_URI);
            StartWithAccessToken(sb);
            AddSteamId(sb, steamID);
            RequestForFriendListByOwnerSteamID result = new RequestForFriendListByOwnerSteamID();
            result.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            result.m_steamID = steamID;
            result.SetIndefiniteCacheFilename(steamID + "_friends");
            if (cached) {
                result.SetRequestAction(RequestActionType.GetFromCacheOnly);
            }
            return result;
        }
        throw new AssertionError();
    }

    public static RequestForUserSummariesBySteamIDs GetRequestForUserSummariesBySteamIDs(String[] steamIDs) {
        if ($assertionsDisabled || IsLoggedIn()) {
            StringBuilder sb = new StringBuilder(((RequestForUserSummariesBySteamIDs_URI.length() + PrecomputeAccessTokenLength()) + PrecomputeKeyLength()) + PrecomputeIdsLength(steamIDs));
            sb.append(RequestForUserSummariesBySteamIDs_URI);
            StartWithAccessToken(sb);
            AddSteamIds(sb, steamIDs);
            RequestForUserSummariesBySteamIDs result = new RequestForUserSummariesBySteamIDs();
            result.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            result.m_steamIDs = (String[]) steamIDs.clone();
            return result;
        }
        throw new AssertionError();
    }

    public static RequestForGroupListByOwnerSteamID GetRequestForGroupListByOwnerSteamID(String steamID, boolean cached) {
        if ($assertionsDisabled || IsLoggedIn()) {
            StringBuilder sb = new StringBuilder(((RequestForGroupListByOwnerSteamID_URI.length() + PrecomputeAccessTokenLength()) + PrecomputeKeyLength()) + steamID.length());
            sb.append(RequestForGroupListByOwnerSteamID_URI);
            StartWithAccessToken(sb);
            AddSteamId(sb, steamID);
            RequestForGroupListByOwnerSteamID result = new RequestForGroupListByOwnerSteamID();
            result.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            result.m_steamID = steamID;
            result.SetIndefiniteCacheFilename(steamID + "_groups");
            if (cached) {
                result.SetRequestAction(RequestActionType.GetFromCacheOnly);
            }
            return result;
        }
        throw new AssertionError();
    }

    public static RequestForGroupsBySteamIDs GetRequestForGroupsBySteamIDs(String[] steamIDs) {
        if ($assertionsDisabled || IsLoggedIn()) {
            StringBuilder sb = new StringBuilder(((RequestForGroupsBySteamIDs_URI.length() + PrecomputeAccessTokenLength()) + PrecomputeKeyLength()) + PrecomputeIdsLength(steamIDs));
            sb.append(RequestForGroupsBySteamIDs_URI);
            StartWithAccessToken(sb);
            AddSteamIds(sb, steamIDs);
            RequestForGroupsBySteamIDs result = new RequestForGroupsBySteamIDs();
            result.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            result.m_steamIDs = (String[]) steamIDs.clone();
            return result;
        }
        throw new AssertionError();
    }

    public static RequestForAppSummariesByAppIDs GetRequestForAppSummariesByAppIDs(String[] appIDs) {
        if ($assertionsDisabled || IsLoggedIn()) {
            StringBuilder sb = new StringBuilder(((RequestForAppSummariesByAppIDs_URI.length() + PrecomputeAccessTokenLength()) + PrecomputeKeyLength()) + PrecomputeIdsLength(appIDs));
            sb.append(RequestForAppSummariesByAppIDs_URI);
            StartWithAccessToken(sb);
            AddAppIds(sb, appIDs);
            RequestForAppSummariesByAppIDs result = new RequestForAppSummariesByAppIDs();
            result.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            result.m_appIDs = (String[]) appIDs.clone();
            return result;
        }
        throw new AssertionError();
    }

    public static void SetCookie2(String cookieName, String cookieValue) {
        String url = Config.URL_COMMUNITY_BASE;
        int iProtocol = url.indexOf("://", 0);
        int iSubDir = url.indexOf("/", iProtocol + 3);
        url = (iSubDir < 0 ? url : url.substring(0, iSubDir)).substring(iProtocol + 3);
        url = "";
        if (!s_cookiesConfiguration.containsKey(url)) {
            s_cookiesConfiguration.put(url, new HashMap());
        }
        Map<String, String> urlCookies = (Map) s_cookiesConfiguration.get(url);
        if (urlCookies.containsKey(cookieName)) {
            if (cookieValue != null) {
                String oldValue = (String) urlCookies.get(cookieName);
                if (oldValue == null || !cookieValue.equals(oldValue)) {
                    s_bCookiesAreOutOfDate = true;
                }
            } else if (urlCookies.get(cookieName) != null) {
                s_bCookiesAreOutOfDate = true;
            }
            urlCookies.remove(cookieName);
            urlCookies.put(cookieName, cookieValue);
            return;
        }
        if (cookieValue != null) {
            s_bCookiesAreOutOfDate = true;
        }
        urlCookies.put(cookieName, cookieValue);
    }

    public static void ResetAllCookies() {
        s_cookiesConfiguration.clear();
        SetCookie2("mobileClient", "android");
        SetCookie2("forceMobile", "1");
        SetCookie2("mobileClientVersion", "" + Config.APP_VERSION_ID + " (" + Config.APP_VERSION + ")");
        SetCookie2("Steam_Language", SteamCommunityApplication.GetInstance().getString(R.string.DO_NOT_LOCALIZE_COOKIE_Steam_Language));
        SetCookie2("steamLogin", null);
        SetCookie2("steamid", null);
        SetCookie2("dob", null);
    }

    public static String GetCookieHeaderString(boolean bSecure) {
        Iterator<Entry<String, Map<String, String>>> urls = s_cookiesConfiguration.entrySet().iterator();
        if (!urls.hasNext()) {
            return null;
        }
        String cookieString = "";
        for (Entry<String, String> entry : ((Map) ((Entry) urls.next()).getValue()).entrySet()) {
            if (bSecure || !((String) entry.getKey()).equals("steamLogin")) {
                try {
                    String sEncodedValue = entry.getValue() != null ? URLEncoder.encode((String) entry.getValue(), "ISO-8859-1") : "";
                    if (!cookieString.equals("")) {
                        cookieString = cookieString + "; ";
                    }
                    cookieString = ((cookieString + ((String) entry.getKey())) + "=") + sEncodedValue;
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        return cookieString;
    }

    public static void SyncAllCookies() {
        if (s_bCookiesAreOutOfDate) {
            s_bCookiesAreOutOfDate = false;
            CookieManager cm = CookieManager.getInstance();
            for (Entry<String, Map<String, String>> urlEntry : s_cookiesConfiguration.entrySet()) {
                String cookieString = "";
                for (Entry<String, String> entry : ((Map) urlEntry.getValue()).entrySet()) {
                    try {
                        String sEncodedValue = entry.getValue() != null ? URLEncoder.encode((String) entry.getValue(), "ISO-8859-1") : "";
                        if (!cookieString.equals("")) {
                            cookieString = cookieString + "; ";
                        }
                        cookieString = (((cookieString + ((String) entry.getKey())) + "=") + sEncodedValue) + "; secure;";
                        cm.setCookie(Config.URL_COMMUNITY_BASE, cookieString);
                        cm.setCookie(Config.URL_STORE_BASE, cookieString);
                        cookieString = "";
                    } catch (UnsupportedEncodingException e) {
                    }
                }
            }
            CookieSyncManager.getInstance().sync();
        }
    }
}
